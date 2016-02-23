/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.komodo.relational.commands.schema;

import java.util.Arrays;
import java.util.List;
import org.komodo.relational.commands.RelationalShellCommand;
import org.komodo.relational.model.Schema;
import org.komodo.shell.api.WorkspaceStatus;

/**
 * A base class for @{link {@link Schema Schema}-related shell commands.
 */
abstract class SchemaShellCommand extends RelationalShellCommand {

    protected static final String RENDITION = "rendition"; //$NON-NLS-1$

    protected static final List< String > ALL_PROPS = Arrays.asList( new String[] { RENDITION } );

    protected SchemaShellCommand( final String name,
                                  final WorkspaceStatus status ) {
        super( status, name );
    }

    protected Schema getSchema() throws Exception {
        assert getContext() instanceof Schema;
        return Schema.RESOLVER.resolve(getTransaction(), getContext());
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.shell.api.ShellCommand#isValidForCurrentContext()
     */
    @Override
    public final boolean isValidForCurrentContext() {
        try {
            return Schema.RESOLVER.resolvable(getTransaction(), getContext());
        } catch (Exception ex) {
            return false;
        }
    }

}