/*************************************************************************************
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership. Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 ************************************************************************************/
package org.komodo.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.modeshape.jcr.api.JcrConstants.JCR_MIXIN_TYPES;
import static org.modeshape.jcr.api.JcrConstants.JCR_PRIMARY_TYPE;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.jcr.Session;
import org.komodo.spi.repository.KomodoObject;
import org.komodo.spi.repository.Property;
import org.komodo.spi.repository.Repository;
import org.komodo.spi.repository.Repository.UnitOfWork;
import org.komodo.test.utils.AbstractLocalRepositoryTest;
import org.modeshape.jcr.JcrSession;
import org.modeshape.jcr.api.JcrConstants;


/**
 * AbstractImporterTest
 */
@SuppressWarnings( {"nls"} )
public abstract class AbstractImporterTest extends AbstractLocalRepositoryTest {

    protected static final String DATA_DIRECTORY = File.separator + "data"; //$NON-NLS-1$

    protected InputStream setup(String fileName) {

        InputStream stream = getClass().getResourceAsStream(DATA_DIRECTORY + File.separator + fileName);
        assertNotNull(stream);
        return stream;
    }

    protected abstract KomodoObject runImporter(Repository repository,
                                                UnitOfWork uow,
                                                InputStream inputStream,
                                                ImportType importType,
                                                ImportOptions importOptions,
                                                ImportMessages importMessages);

    protected abstract KomodoObject runImporter(Repository repository, 
                                                UnitOfWork uow,
                                                File file,
                                                ImportType importType,
                                                ImportOptions importOptions,
                                                ImportMessages importMessages);

    protected KomodoObject runImporter(Repository repository, UnitOfWork uow, Object content,
                                                                 ImportType importType, ImportOptions importOptions,
                                                                 ImportMessages importMessages) {
        if (content instanceof File)
            return runImporter(repository, uow, (File) content, importType, importOptions, importMessages);
        else if (content instanceof InputStream)
            return runImporter(repository, uow, (InputStream) content, importType, importOptions, importMessages);

        fail("Content should be a file or input stream");
        return null;
    }
    
    protected KomodoObject executeImporter(Object content, ImportType importType,
                                                                        ImportOptions importOptions,
                                                                        ImportMessages importMessages, String... sequencePaths)
                                                                        throws Exception {
        assertNotNull(_repo);
        assertNotNull(content);
        assertNotNull(importType);
        assertNotNull(importOptions);
        assertNotNull(importMessages);

        UnitOfWork uow = _repo.createTransaction("test-importer", false, null);
        Session session = session(uow);

        CountDownLatch updateLatch = addSequencePathListener(uow, sequencePaths);

        KomodoObject kObject = runImporter(_repo, uow, content, importType, importOptions, importMessages);
        if (importMessages.hasError()) {
            return null;
        }

        //
        // Save the session to ensure that the sequencers are executed.
        //
        // However, we cannot logout:
        // * Sequence listeners added through the observation manager are only applicable for the lifetime
        //    of the session.
        // * Once session.logout() is called, then all listeners are removed from the RepositoryChangeBus so
        //    sequencing will fire a completion event but the sequence listener will never receive it.
        //
        session.save();

        // Wait for the sequencing of the repository or timeout of 3 minutes
        assertTrue(updateLatch.await(3, TimeUnit.MINUTES));

        //
        // Commit and logout of the session after waiting for the sequence listener
        // to observe the completion of the sequencing
        //
        uow.commit();

        traverse(kObject);

        return kObject;
    }

    protected String enc(String input) throws Exception {
        UnitOfWork uow = _repo.createTransaction("test-encode-transaction", true, null);
        try {
            return ((JcrSession)session(uow)).encode(input);
        } finally {
            uow.commit();
        }
    }

    protected void verifyProperty(KomodoObject node, String propertyName, String... expectedValues) throws Exception {
        Property property = node.getProperty(null, propertyName);
        assertNotNull(property);

        List<String> values;
        if (property.isMultiple(null))
            values = Arrays.asList(property.getStringValues(null));
        else {
            values = new ArrayList<String>();
            values.add(property.getStringValue(null));
        }

        assertEquals(expectedValues.length, values.size());
        for (String expectedValue : expectedValues) {
            assertTrue(values.contains(expectedValue));
        }
    }

    protected void verifyPrimaryType(KomodoObject node, String expectedValue) throws Exception {
        verifyProperty(node, JCR_PRIMARY_TYPE, expectedValue);
    }

    protected void verifyMixinType(KomodoObject node, String... expectedValues) throws Exception {
        Property property = node.getProperty(null, JCR_MIXIN_TYPES);
        assertNotNull(property);

        List<String> values;
        if (property.isMultiple(null))
            values = Arrays.asList(property.getStringValues(null));
        else {
            values = new ArrayList<String>();
            values.add(property.getStringValue(null));
        }

        assertEquals(expectedValues.length, values.size());
        for (String expectedValue : expectedValues) {
            assertTrue(values.contains(expectedValue));
        }
    }

    protected void verifyBaseProperties(KomodoObject node, String primaryType, String mixinType) throws Exception {
        verifyPrimaryType(node, primaryType);
        if (mixinType == null)
            return;

        // Only if mixinType is not null do we check it
        verifyMixinType(node, mixinType);
    }

    protected KomodoObject verify(KomodoObject parentNode, String relativePath, String primaryType, int index, String mixinType) throws Exception {
        String indexExp = EMPTY_STRING;
        if (index > -1)
            indexExp = OPEN_SQUARE_BRACKET + index + CLOSE_SQUARE_BRACKET;

        KomodoObject childNode = null;
        if (parentNode.hasChild(null, relativePath)) {
            childNode = parentNode.getChild(null, relativePath + indexExp);
        } else childNode = parentNode.getChild(null, enc(relativePath) + indexExp);
        assertNotNull(childNode);

        verifyBaseProperties(childNode, primaryType, mixinType);
        return childNode;
    }

    protected KomodoObject verify(KomodoObject parentNode, String relativePath, String primaryType, String mixinType) throws Exception {
        return verify(parentNode, relativePath, primaryType, -1, mixinType);
    }

    protected KomodoObject verify(KomodoObject parentNode, String relativePath, String primaryType) throws Exception {
        return verify(parentNode, relativePath, primaryType, -1, null);
    }

    protected KomodoObject verify(KomodoObject parentNode, String relativePath) throws Exception {
        return verify(parentNode, relativePath, JcrConstants.NT_UNSTRUCTURED, -1, null);
    }
}
