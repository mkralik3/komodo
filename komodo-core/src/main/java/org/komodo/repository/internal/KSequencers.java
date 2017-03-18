/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */
package org.komodo.repository.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import org.komodo.core.KomodoLexicon;
import org.komodo.core.Messages;
import org.komodo.repository.KSequencerController;
import org.komodo.repository.KSequencerListener;
import org.komodo.spi.constants.StringConstants;
import org.komodo.spi.lexicon.TeiidSqlLexicon;
import org.komodo.utils.KLog;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.Session;
import org.teiid.modeshape.sequencer.dataservice.lexicon.DataVirtLexicon;
import org.teiid.modeshape.sequencer.ddl.StandardDdlLexicon;
import org.teiid.modeshape.sequencer.ddl.TeiidDdlLexicon;
import org.teiid.modeshape.sequencer.vdb.lexicon.VdbLexicon;


/**
 * Sequencers class responsible for executing all the sequencers in
 * consecutive, synchronous order.
 */
public class KSequencers implements StringConstants, EventListener, KSequencerController {

    private final WorkspaceIdentifier identifier;

    private Session session;

    // Flag switched on only when a sequencing execution is started
    private boolean sequencingActive = false;

    // List appended to by running sequencers detailing their unique identifiers
    private List<String> runningSequencers = new ArrayList<>();

    private Set<KSequencerListener> listeners = new HashSet<>();

    /**
     * Create new instance
     *
     * @param identifier the workspace identifier
     * @throws Exception if error occurs
     */
    public KSequencers(WorkspaceIdentifier identifier) throws Exception {
        this.identifier = identifier;
        this.session = ModeshapeUtils.createSession(identifier);
        KLog.getLogger().debug("KSequencers.init: session = {0}", session.hashCode()); //$NON-NLS-1$

        ObservationManager manager = session.getWorkspace().getObservationManager();
        manager.addEventListener(this,
                                 Event.NODE_ADDED |
                                 Event.NODE_MOVED |
                                 Event.NODE_REMOVED |
                                 Event.PROPERTY_ADDED |
                                 Event.PROPERTY_CHANGED |
                                 Event.PROPERTY_REMOVED,
                                 null,                                           // ignore non-komodo paths
                                 true,                                          // deep to look beneath komodo root
                                 null,                                          // all uuids
                                 null,                                          // all node types
                                 true);                                        // ignore events generated by this session
    }

    /**
     * Dispose of this instance
     */
    @Override
    public synchronized void dispose() {
        if (session != null) {
            KLog.getLogger().debug("KSequencers.dispose: logout session: {0}", session.hashCode()); //$NON-NLS-1$
            session.logout();
            session = null;
        }
    }

    /**
     * @param listener the listener to add
     */
    @Override
    public synchronized void addSequencerListener(KSequencerListener listener) throws Exception {
        //
        // Events are passed the user data using the observation manager so
        // to identifier the event with this commit request, set the user data
        // to the request id.
        //
        // This connects the sequencer listener constructed above with the
        // events being generated from saving the session, ensuring that the
        // correct listener is notified.
        //
        String id = listener.id();
        javax.jcr.Session session = listener.session();

        session.getWorkspace().getObservationManager().setUserData(id);

        listeners.add(listener);
    }

    /**
     * @return the identifier
     */
    public WorkspaceIdentifier getIdentifier() {
        return this.identifier;
    }

    private boolean isVdbSequenceable(Node node, String propertyName) {
        try {
            if (! propertyName.equals(JcrConstants.JCR_DATA))
                return false;

            if (! node.getName().equals(JcrConstants.JCR_CONTENT))
                return false;

            Node parentNode = node.getParent();
            if (parentNode == null ||
                    ! (parentNode.getPrimaryNodeType().getName().equals(VdbLexicon.Vdb.VIRTUAL_DATABASE)))
                return false;
        } catch (RepositoryException ex) {
            return false;
        }

        return true;
    }

    private boolean isVdbSequenceable(Property property) {
        try {
            return isVdbSequenceable(property.getParent(), property.getName());
        } catch (RepositoryException ex) {
            return false;
        }
    }

    private boolean isConnectionSequenceable(Node node, String propertyName) {
        try {
            if ( !propertyName.equals( JcrConstants.JCR_DATA ) ) {
                return false;
            }

            if ( !node.getName().equals( JcrConstants.JCR_CONTENT ) ) {
                return false;
            }

            // make sure output node is a connection node
            final Node parentNode = node.getParent();

            return ( ( parentNode != null )
                     && parentNode.getPrimaryNodeType().getName().equals( DataVirtLexicon.Connection.NODE_TYPE ) );
        } catch ( final RepositoryException e ) {
            KLog.getLogger().error( "KSequencers.isConnectionSequenceable", e ); //$NON-NLS-1$
            return false;
        }
    }

    private boolean isConnectionSequenceable( final Property property ) {
        try {
            return isConnectionSequenceable(property.getParent(), property.getName());
        } catch (RepositoryException ex) {
            return false;
        }
    }

    private boolean isDataServiceSequenceable( Node node, String propertyName ) {
        try {
            if ( !propertyName.equals( JcrConstants.JCR_DATA ) ) {
                return false;
            }

            if ( !node.getName().equals( JcrConstants.JCR_CONTENT ) ) {
                return false;
            }

            // make sure output node is a data service node
            final Node parentNode = node.getParent();

            return ( ( parentNode != null )
                     && parentNode.getPrimaryNodeType().getName().equals( DataVirtLexicon.DataService.NODE_TYPE ) );
        } catch ( final RepositoryException e ) {
            KLog.getLogger().error( "KSequencers.isDataServiceSequenceable", e ); //$NON-NLS-1$
            return false;
        }
    }

    private boolean isDataServiceSequenceable( final Property property ) {
        try {
            return isDataServiceSequenceable(property.getParent(), property.getName());
        } catch (RepositoryException ex) {
            return false;
        }
    }

    private boolean isDdlSequenceable(Node node, String propertyName) {
        try {
            List<String> nodeTypeNames = ModeshapeUtils.getAllNodeTypeNames(node);

            if (propertyName.equals(VdbLexicon.Model.MODEL_DEFINITION) &&
                    nodeTypeNames.contains(VdbLexicon.Vdb.DECLARATIVE_MODEL))
                return true;

            if (propertyName.equals(KomodoLexicon.Schema.RENDITION) &&
                    nodeTypeNames.contains(KomodoLexicon.Schema.NODE_TYPE))
                return true;
        } catch (RepositoryException ex) {
            // Not required to be logged since false is returned anyway
        }

        return false;
    }

    private boolean isDdlSequenceable(Property property) {
        try {
            return isDdlSequenceable(property.getParent(), property.getName());
        } catch (RepositoryException ex) {
            return false;
        }
    }

    private boolean isTsqlSequenceable(Node node, String propertyName) {
        try {
            List<String> nodeTypeNames = ModeshapeUtils.getAllNodeTypeNames(node);

            if (propertyName.equals(TeiidDdlLexicon.CreateTable.QUERY_EXPRESSION) &&
                    (   nodeTypeNames.contains(TeiidDdlLexicon.CreateTable.TABLE_STATEMENT) ||
                        nodeTypeNames.contains(TeiidDdlLexicon.CreateTable.VIEW_STATEMENT)))
                return true;

            if (propertyName.equals(TeiidDdlLexicon.CreateProcedure.STATEMENT) &&
                    nodeTypeNames.contains(TeiidDdlLexicon.CreateProcedure.PROCEDURE_STATEMENT))
                return true;
        } catch (RepositoryException ex) {
            // Not required to be logged since false is returned anyway
        }

        return false;
    }

    private boolean isTsqlSequenceable(Property property) {
        try {
            return isTsqlSequenceable(property.getParent(), property.getName());
        } catch (RepositoryException ex) {
            return false;
        }
    }

    private SequencerType isSequenceable(Node node, String propertyName) {
        if (isVdbSequenceable(node, propertyName))
            return SequencerType.VDB;

        if (isDdlSequenceable(node, propertyName))
            return SequencerType.DDL;

        if (isTsqlSequenceable(node, propertyName))
            return SequencerType.TSQL;

        if (isDataServiceSequenceable(node, propertyName)) {
            return SequencerType.DATA_SERVICE;
        }

        if (isConnectionSequenceable(node, propertyName)) {
            return SequencerType.CONNECTION;
        }

        return null;
    }

    /**
     * @param property
     * @return if the property is sequenceable then return the sequencer type, otherwise null
     */
    private SequencerType isSequenceable(Property property) {
        if (isVdbSequenceable(property))
            return SequencerType.VDB;

        if (isDdlSequenceable(property))
            return SequencerType.DDL;

        if (isTsqlSequenceable(property))
            return SequencerType.TSQL;

        if ( isDataServiceSequenceable( property ) ) {
            return SequencerType.DATA_SERVICE;
        }

        if ( isConnectionSequenceable( property ) ) {
            return SequencerType.CONNECTION;
        }

        return null;
    }

    private boolean checkSequencerWork(SequencerType sequencerType, Node oldNode, Node newNode) throws Exception {
        switch (sequencerType) {
            case VDB:
                return newNode.hasProperty(VdbLexicon.Vdb.VERSION);
            case DDL:
            case TSQL:
                return ModeshapeUtils.childrenCount(oldNode) < ModeshapeUtils.childrenCount(newNode);
            case CONNECTION:
                return newNode.hasProperty( DataVirtLexicon.Connection.TYPE );
            case DATA_SERVICE:
                return newNode.hasNodes();
            default:
                break;
        }

        return false;
    }

    private void preSequenceClean(SequencerType sequencerType, Node outputNode) throws Exception {
        Session session = null;

        try {
            switch (sequencerType) {
                case VDB:
                {
                    if (! outputNode.hasNodes())
                        return;

                    session = ModeshapeUtils.createSession(getIdentifier());
                    KLog.getLogger().debug("KSequencers.preSequenceClean: session = {0}", session.hashCode()); //$NON-NLS-1$
                    NodeIterator childIter = outputNode.getNodes();
                    while(childIter.hasNext()) {
                        Node child = childIter.nextNode();
                        if (! ModeshapeUtils.hasTypeNamespace(child, VdbLexicon.Namespace.PREFIX))
                            continue;

                        Node wChild = session.getNode(child.getPath());
                        wChild.remove();
                    }
                    return;
                }
                case DDL:
                {
                    session = ModeshapeUtils.createSession(getIdentifier());
                    Node parent = session.getNode(outputNode.getPath());
                    NodeIterator children = parent.getNodes();
                    while(children.hasNext()) {
                        Node child = children.nextNode();
                        if (! ModeshapeUtils.hasTypeNamespace(child, TeiidDdlLexicon.Namespace.PREFIX))
                            continue;

                        child.remove();
                    }
                    return;
                }
                case TSQL:
                case DATA_SERVICE:
                case CONNECTION:
                {
                    session = ModeshapeUtils.createSession(getIdentifier());
                    Node parent = session.getNode(outputNode.getPath());
                    NodeIterator children = parent.getNodes();
                    while(children.hasNext()) {
                        Node child = children.nextNode();
                        if (! ModeshapeUtils.hasTypeNamespace(child, TeiidSqlLexicon.Namespace.PREFIX))
                            continue;

                        child.remove();
                    }
                    return;
                }
            }
        } finally {
            if (session != null && session.isLive()) {
                if (session.hasPendingChanges())
                    session.save();

                session.logout();
            }
        }
    }

    private void analyseDdlNodes(Node node) throws Exception {
        if (node == null)
            return;

        List<NodeType> types = new ArrayList<>();
        types.add(node.getPrimaryNodeType());
        types.addAll(Arrays.asList(node.getMixinNodeTypes()));

        for (NodeType type : types) {
            if (StandardDdlLexicon.TYPE_UNKNOWN_STATEMENT.equals(type.getName())) {
                Property property = node.getProperty(StandardDdlLexicon.DDL_EXPRESSION);
                String invalidDdl = property.getString();
                throw new Exception(Messages.getString(Messages.KSequencers.Unknown_Message) + NEW_LINE + invalidDdl);
            }

            if (StandardDdlLexicon.TYPE_PROBLEM.equals(type.getName())) {
                Property problemLevel = node.getProperty(StandardDdlLexicon.PROBLEM_LEVEL);
                Property problemMsg = node.getProperty(StandardDdlLexicon.MESSAGE);

                throw new Exception(Messages.getString(Messages.KSequencers.Problem_Message, problemLevel) + NEW_LINE + problemMsg);
            }
        }

        NodeIterator nodes = node.getNodes();
        while(nodes.hasNext())
            analyseDdlNodes(nodes.nextNode());

    }

    private void analyseSequencerResults(SequencerType sequencerType, Node rootNode) throws Exception {
        if (SequencerType.DDL != sequencerType)
            return;

        analyseDdlNodes(rootNode);
    }

    private void sequence(SequencerType sequencerType, Property property,
                                                 Node outputNode, String eventId) throws Exception {
        KLog.getLogger().debug("Executing pre-sequencing of " + sequencerType.name() + " Sequencer for property " + property.getName());  //$NON-NLS-1$//$NON-NLS-2$
        preSequenceClean(sequencerType, outputNode);

        Session seqSession = ModeshapeUtils.createSession(getIdentifier());
        KLog.getLogger().debug("KSequencers.sequenceClean: session = {0}", session.hashCode()); //$NON-NLS-1$

        try {
            KLog.getLogger().debug("Executing " + sequencerType.name() + " Sequencer on property " + property.getName());  //$NON-NLS-1$//$NON-NLS-2$

            Property seqProperty = seqSession.getProperty(property.getPath());
            Node seqOutputNode = seqSession.getNode(outputNode.getPath());

            boolean status = seqSession.sequence(sequencerType.toString(), seqProperty, seqOutputNode);
            if (!status)
                KLog.getLogger().error("The sequence " + sequencerType.name() + " failed in some way");
            else {
                //
                // Return flag is only a notional indicator that the sequencer executed successfully.
                // Need to confirm that changes have actually been made to the output node.
                //
                status = checkSequencerWork(sequencerType, outputNode, seqOutputNode);
                if (status) {
                    // Sequencer executed and changed something

                    // Create an identifier for this sequencer's work
                    String seqPropId = encode(eventId, sequencerType, property);

                    // Adds the identifier to the user data for the 'next' event to be received by this listener
                    seqSession.getWorkspace().getObservationManager().setUserData(seqPropId);

                    // Adds the identifier to the running sequencers to indicate work has been done and need to
                    // wait for the event to run through before proclaiming eveything is complete
                    runningSequencers.add(seqPropId);

                    try {
                        analyseSequencerResults(sequencerType, seqOutputNode);
                    } finally {
                        // Save this session
                        seqSession.save();
                    }
                }
            }

        } finally {
            if (seqSession != null && seqSession.isLive())
                seqSession.logout();
        }
    }

    private String encode(String eventId, SequencerType sequencerType, Property property) throws Exception {
        return eventId + HYPHEN + sequencerType.name() + HYPHEN + property.getPath();
    }

    private Node sequencedOutput(SequencerType sequencerType, Node outputNode)
        throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        switch (sequencerType) {
            case VDB:
            case CONNECTION:
            case DATA_SERVICE:
                outputNode = outputNode.getParent();
                break;
            default:
                break;
        }
        return outputNode;
    }

    private void sequence(SequencerType sequencerType, Property property, String eventId) throws Exception {
        sequencingActive = true;

        Node outputNode = property.getParent();

        outputNode = sequencedOutput(sequencerType, outputNode);

        sequence(sequencerType, property, outputNode, eventId);
    }

    private void notifySequencerCompletion(String eventUserData) {
        Iterator<KSequencerListener> iterator = listeners.iterator();
        while(iterator.hasNext()) {
            KSequencerListener listener = iterator.next();

            if (! listener.session().isLive()) {
                iterator.remove();
                continue;
            }

            if (eventUserData == null || ! eventUserData.startsWith(listener.id()))
                continue; // Listener is not listening for this event

            KLog.getLogger().debug("KSequencers complete. Notifying " + listener); //$NON-NLS-1$
            listener.sequencingCompleted();
        }
    }

    private void notifySequencerError(String eventUserData, Exception exception) {
        Iterator<KSequencerListener> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            KSequencerListener listener = iterator.next();

            if (! listener.session().isLive()) {
                iterator.remove();
                continue;
            }

            if (eventUserData == null || ! eventUserData.startsWith(listener.id()))
                continue; // Listener is not listening for this event

            KLog.getLogger().debug("KSequencers error. Notifying " + listener + " of exception", exception); //$NON-NLS-1$ //$NON-NLS-2$
            listener.sequencingError(exception);
        }
    }

    @Override
    public void onEvent(EventIterator events) {
        KLog.getLogger().debug("KSequencers: onEvent() called"); //$NON-NLS-1$

        String eventUserData = null;
        try {
            int eventNo = 0;
            int systemEvents = 0;
            while (events.hasNext()) {
                eventNo++;

                KLog.getLogger().debug("KSequencers: Event in loop - " + eventNo); //$NON-NLS-1$
                Event event = events.nextEvent();
                String eventPath = event.getPath();
                eventUserData = event.getUserData();

                //
                // Ignore any event from jcr:system since they are internal system events
                // An example would be when namespaces are saved into modeshape's
                // internal namespace store under /system/namespaces
                //
                if(eventPath.startsWith(FORWARD_SLASH + JcrLexicon.SYSTEM.getString())) {
                    systemEvents++;
                    continue;
                }

                switch (event.getType()) {
                    case Event.NODE_ADDED:
                    case Event.NODE_MOVED:
                    case Event.NODE_REMOVED:
                        //
                        // Even though we do nothing with these events the
                        // sequencer still must fire on them in order to ensure the
                        // listeners are always notified that the sequencer has finished
                        //
                        continue;
                    case Event.PROPERTY_ADDED:
                    case Event.PROPERTY_CHANGED:
                    {
                        KLog.getLogger().debug("KSequencers: processing event " + eventUserData + " for path " + eventPath); //$NON-NLS-1$ //$NON-NLS-2$

                        if (! session.propertyExists(eventPath)) {
                            // property never got as far as being visible to this session
                            // implies modeshape is shutting down maybe
                            continue;
                        }

                        Property property = session.getProperty(eventPath);
                        SequencerType sequencerType = isSequenceable(property);
                        if (sequencerType == null)
                            continue;

                        sequence(sequencerType, property, eventUserData);
                        continue;
                    }
                    case Event.PROPERTY_REMOVED:
                    {
                        KLog.getLogger().debug("KSequencers: processing property removal event " + eventUserData + " for path " + eventPath); //$NON-NLS-1$ //$NON-NLS-2$
                        int lastSlash = eventPath.lastIndexOf(FORWARD_SLASH);
                        if (lastSlash == -1)
                            continue; // Not going to be a sequenceable item if path contains no slashes

                        String propertyName = eventPath.substring(lastSlash + 1);
                        String nodePath = eventPath.substring(0, lastSlash);

                        if (! session.nodeExists(nodePath))
                            continue; // Parent node has been removed as well so not worth worrying about removing children

                        Node node = session.getNode(nodePath);
                        SequencerType sequencerType = isSequenceable(node, propertyName);
                        if (sequencerType == null)
                            continue; // Not a sequenceable property so no need to worry about cleanup

                        //
                        // Sequencers used different ouput node
                        //
                        node = sequencedOutput(sequencerType, node);

                        //
                        // Clean all the children that the sequencer was responsible for creating
                        //
                        preSequenceClean(sequencerType, node);
                    }
                }
            }

            //
            // Event looping has completed.
            //
            if (systemEvents == eventNo) {
                //
                // This set of events was wholly internal to modeshape and
                // should not be reported out to any listeners. This can cause
                // listeners to prematurely complete despite the 'next' event
                // being the sequencing event that those listeners are actually
                // awaiting.
                //
                return;
            }

            if (! sequencingActive) {
                notifySequencerCompletion(eventUserData);
                return; // No sequencers started so nothing further to do
            }

            //
            // Sequencers add a user-data object to their events [ see sequence(SequencerType, Property, Node) ].
            // The object is the same as that added to runningSequencers
            //
            // If this set of events has a user-data object, try removing it from runningSequencers
            // since this confirms that this set of events is the completion of that particular sequencer.
            //
            if (eventUserData != null) {
                boolean removed = runningSequencers.remove(eventUserData);
                if (removed)
                    KLog.getLogger().debug("Sequencer with id " + eventUserData + " has completed"); //$NON-NLS-1$ //$NON-NLS-2$
            }

            //
            // Determine if runningSequencers is empty. This can happen if
            // a) no sequencers had been running (in which case sequencingActive would be false)
            // b) the last sequencer identifier has been removed
            //
            if (runningSequencers.isEmpty()) {
                //
                // All sequencers have completed so reset sequencingActive
                //
                sequencingActive = false;

                //
                // Notify clients that sequencing has completed
                //
                notifySequencerCompletion(eventUserData);
            } else {
                //
                // Still sequencers are currently executing
                //
                if (KLog.getLogger().isDebugEnabled()) {
                    StringBuffer buffer = new StringBuffer("Current Sequencing Train: "); //$NON-NLS-1$
                    for (String id : runningSequencers)
                        buffer.append(id).append(TAB);

                    KLog.getLogger().debug(buffer.toString());
                }
            }
        } catch (Throwable t) {
            sequencingActive = false;
            runningSequencers.clear();
            Exception ex;
            if (t instanceof Exception)
                ex = (Exception) t;
            else
                ex = new Exception(t);

            notifySequencerError(eventUserData, ex);
            return;
        }
    }
}
