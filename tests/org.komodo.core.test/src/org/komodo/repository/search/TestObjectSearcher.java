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
package org.komodo.repository.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.List;
import org.junit.Test;
import org.komodo.core.KomodoLexicon;
import org.komodo.modeshape.teiid.cnd.TeiidSqlLexicon;
import org.komodo.repository.RepositoryImpl;
import org.komodo.repository.RepositoryTools;
import org.komodo.spi.repository.KomodoObject;
import org.komodo.spi.repository.Property;
import org.komodo.spi.repository.Repository.KeywordCriteria;
import org.komodo.test.utils.AbstractLocalRepositoryTest;
import org.modeshape.jcr.api.JcrConstants;

@SuppressWarnings( {"nls", "javadoc"} )
public class TestObjectSearcher extends AbstractLocalRepositoryTest {

    @Test
    public void shouldHaveRepository() {
        try {
            new ObjectSearcher(null);
            fail("Repository expected");
        } catch (Exception ex) {
            assertTrue(ex instanceof IllegalArgumentException);
        }
    }

    @Test
    public void noParameters() {
        try {
            ObjectSearcher os = new ObjectSearcher(_repo);
            os.toString();
            fail("ObjectSearcher should require at least 1 From Clause");
        } catch (Exception ex) {
            assertTrue(ex instanceof IllegalArgumentException);
        }

        try {
            ObjectSearcher os = new ObjectSearcher(_repo);
            os.toString();
            fail("ObjectSearcher should require at least 1 From Clause");
        } catch (Exception ex) {
            assertTrue(ex instanceof IllegalArgumentException);
        }
    }

    @Test
    public void oneFrom() throws Exception {
        String expected = "SELECT [jcr:path], [mode:localName] FROM [tko:workspace]";
        ObjectSearcher os = new ObjectSearcher(_repo);
        os.addFromType(KomodoLexicon.Komodo.WORKSPACE, null);
        assertEquals(expected, os.toString());
    }

    @Test
    public void twoFroms() throws Exception {
        String expected = "SELECT [jcr:path], [mode:localName] FROM [tko:workspace], [tko:library]";
        ObjectSearcher os = new ObjectSearcher(_repo);
        os.addFromType(KomodoLexicon.Komodo.WORKSPACE);
        os.addFromType(KomodoLexicon.Komodo.LIBRARY);
        assertEquals(expected, os.toString());
    }

    @Test
    public void addWhereClauseWithNullType() {
        try {
            ObjectSearcher os = new ObjectSearcher(_repo);
            os.addWhereSetClause(null, "p1", "property1", (String[]) null);
            os.toString();
            fail("Type cannot be null");
        } catch (Exception ex) {
            assertTrue(ex instanceof IllegalArgumentException);
        }
    }

    @Test
    public void addWhereClauseWithNullAliasNoFrom() {
        try {
            ObjectSearcher os = new ObjectSearcher(_repo);
            os.addWhereSetClause(null, null, "property1", "value1");
            os.toString();
            fail("Alias cannot be null if there are no from clauses yet!");
        } catch (Exception ex) {
            assertTrue(ex instanceof IllegalArgumentException);
        }
    }

    @Test
    public void addWhereClauseWithNullAliases() {
        try {
            ObjectSearcher os = new ObjectSearcher(_repo);
            os.addFromType(KomodoLexicon.Komodo.WORKSPACE);
            os.addWhereSetClause(null, null, "property1", "value1");
            os.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            assertTrue(ex instanceof IllegalArgumentException);
        }
    }

    @Test
    public void addWhereClauseWithNullAliasWithOneFrom() throws Exception {
        String expected = "SELECT [jcr:path], [mode:localName] FROM [nt:unstructured] AS p1 " +
                                     "WHERE p1.[property1] IN ('value1')";

        ObjectSearcher os = new ObjectSearcher(_repo);
        os.addFromType(JcrConstants.NT_UNSTRUCTURED, "p1");
        os.addWhereSetClause(null, null, "property1", "value1");

        assertEquals(expected, os.toString());
    }

    @Test
    public void addWhereClauseWithInvalidAlias() throws Exception {
        try {
            ObjectSearcher os = new ObjectSearcher(_repo);
            os.addFromType(KomodoLexicon.Komodo.WORKSPACE);
            os.addWhereSetClause(null, "p2", "property1", "value1");
            os.toString();
            fail("Alias of where clause must refer to existing alias");
        } catch (Exception ex) {
            assertTrue(ex instanceof IllegalArgumentException);
        }

        try {
            ObjectSearcher os = new ObjectSearcher(_repo);
            os.addFromType(KomodoLexicon.Komodo.WORKSPACE, "p1");
            os.addWhereSetClause(null, "p2", "property1", "value1");
            os.toString();
            fail("Alias of where clause must refer to existing alias");
        } catch (Exception ex) {
            assertTrue(ex instanceof IllegalArgumentException);
        }
    }

    @Test
    public void addWhereClauseTwoPropertyValues() throws Exception {
        String expected = "SELECT [jcr:path], [mode:localName] FROM [nt:unstructured] AS p1 " +
                                    "WHERE p1.[property1] IN ('value1', 'value2')";
        ObjectSearcher os = new ObjectSearcher(_repo);
        os.addFromType(JcrConstants.NT_UNSTRUCTURED, "p1");
        os.addWhereSetClause(null, "p1", "property1", "value1", "value2");

        assertEquals(expected, os.toString());
    }

    @Test
    public void addTwoFromTypes() throws Exception {
        String expected = "SELECT [jcr:path], [mode:localName] FROM [tko:vdb], [tsql:notCriteria]";
        ObjectSearcher os = new ObjectSearcher(_repo);
        os.addFromType(KomodoLexicon.Vdb.NODE_TYPE);
        os.addFromType(TeiidSqlLexicon.NotCriteria.ID);

        assertEquals(expected, os.toString());
    }

    @Test
    public void addWhereClauseWildcardProperty() throws Exception {
        String expected = "SELECT [jcr:path], [mode:localName] FROM [nt:unstructured] AS p1 " +
                                     "WHERE p1.* IN ('value1', 'value2')";
        ObjectSearcher os = new ObjectSearcher(_repo);
        os.addFromType(JcrConstants.NT_UNSTRUCTURED, "p1");
        os.addWhereSetClause(null, "p1", STAR, "value1", "value2");

        assertEquals(expected, os.toString());
    }

    @Test
    public void addWhereClauseTwoDifferentProperties() throws Exception {
        String expected = "SELECT [jcr:path], [mode:localName] FROM [nt:unstructured] AS p1 " +
                                     "WHERE p1.[property1] IN ('value1', 'value2') " +
                                     "OR p1.[name] IN ('bob', 'bryan')";
        ObjectSearcher os = new ObjectSearcher(_repo);
        os.addFromType(JcrConstants.NT_UNSTRUCTURED, "p1");
        os.addWhereSetClause(null, "p1", "property1", "value1", "value2");
        os.addWhereSetClause(LogicalOperator.OR, "p1", "name", "bob", "bryan");

        assertEquals(expected, os.toString());
    }

    @Test
    public void addWhereClauseOneSetPropertyOneContainsProperty() throws Exception {
        String expected = "SELECT [jcr:path], [mode:localName] FROM [nt:unstructured] AS p1 " +
                                     "WHERE p1.[property1] IN ('value1', 'value2') " +
                                     "OR CONTAINS(p1.[name], 'bob')";
        ObjectSearcher os = new ObjectSearcher(_repo);
        os.addFromType(JcrConstants.NT_UNSTRUCTURED, "p1");
        os.addWhereSetClause(null, "p1", "property1", "value1", "value2");
        os.addWhereContainsClause(LogicalOperator.OR, "p1", "name", "bob");

        assertEquals(expected, os.toString());
    }

    @Test
    public void addWhereClauseContainsWildcardProperty() throws Exception {
        String expected = "SELECT [jcr:path], [mode:localName] FROM [nt:unstructured] AS p1 " +
                                     "WHERE p1.* IN ('value1', 'value2')";
        ObjectSearcher os = new ObjectSearcher(_repo);
        os.addFromType(JcrConstants.NT_UNSTRUCTURED, "p1");
        os.addWhereSetClause(null, "p1", STAR, "value1", "value2");

        assertEquals(expected, os.toString());
    }

    @Test
    public void addWhereClauseContainsPropertyWithTwoKeywords() throws Exception {
        String expected = "SELECT [jcr:path], [mode:localName] FROM [nt:unstructured] AS p1 " +
                                     "WHERE p1.[property1] IN ('value1', 'value2') " +
                                     "OR CONTAINS(p1.[name], 'bob OR chris')";
        ObjectSearcher os = new ObjectSearcher(_repo);
        os.addFromType(JcrConstants.NT_UNSTRUCTURED, "p1");
        os.addWhereSetClause(null, "p1", "property1", "value1", "value2");
        os.addWhereContainsClause(LogicalOperator.OR, "p1", "name", KeywordCriteria.ANY, "bob", "chris");

        assertEquals(expected, os.toString());
    }

    @Test
    public void executeSingleFromQuery() throws Exception {
        assertNotNull(_repo);

        // Create the komodo workspace
        KomodoObject workspace = _repo.komodoWorkspace(getTransaction());
        assertNotNull(workspace);

        workspace.addChild(getTransaction(), "test1", KomodoLexicon.VdbModel.NODE_TYPE);
        workspace.addChild(getTransaction(), "test2", KomodoLexicon.VdbModel.NODE_TYPE);
        workspace.addChild(getTransaction(), "test3", KomodoLexicon.VdbModel.NODE_TYPE);
        workspace.addChild(getTransaction(), "test4", KomodoLexicon.VdbModel.NODE_TYPE);
        workspace.addChild(getTransaction(), "test5", KomodoLexicon.VdbModel.NODE_TYPE);

        KomodoObject[] testNodes = workspace.getChildrenOfType(getTransaction(), KomodoLexicon.VdbModel.NODE_TYPE);
        assertEquals(5, testNodes.length);

        commit(); // must commit for search queries to work

        ObjectSearcher os = new ObjectSearcher(_repo);
        os.addFromType(KomodoLexicon.VdbModel.NODE_TYPE);
        List<KomodoObject> searchObjects = os.searchObjects(getTransaction());
        assertEquals(testNodes.length, searchObjects.size());
    }

    @Test
    public void executeFromQueryWithWhere() throws Exception {
        assertNotNull(_repo);

        // Create the komodo workspace
        KomodoObject workspace = _repo.komodoWorkspace(getTransaction());
        assertNotNull(workspace);

        for (int i = 1; i < 6; ++i) {
            KomodoObject child = workspace.addChild(getTransaction(), "test" + i, KomodoLexicon.VdbModel.NODE_TYPE);
            child.setProperty(getTransaction(), KomodoLexicon.VdbModel.MODEL_DEFINITION, "DDL");
        }

        KomodoObject[] testNodes = workspace.getChildrenOfType(getTransaction(), KomodoLexicon.VdbModel.NODE_TYPE);
        assertEquals(5, testNodes.length);
        for (KomodoObject testKO : testNodes) {
            Property property = testKO.getProperty(getTransaction(), KomodoLexicon.VdbModel.MODEL_DEFINITION);
            assertEquals("DDL", property.getStringValue(getTransaction()));
        }

        ObjectSearcher os = new ObjectSearcher(_repo);
        os.addFromType(JcrConstants.NT_UNSTRUCTURED, "nt");
        os.addWhereContainsClause(null, "nt", KomodoLexicon.VdbModel.MODEL_DEFINITION, "DDL");

        String expected = "SELECT [jcr:path], [mode:localName] FROM [nt:unstructured] AS nt " +
                                     "WHERE CONTAINS(nt.[vdb:modelDefinition], 'DDL')";
        assertEquals(expected, os.toString());

        commit(); // must commit for search queries to work

        List<KomodoObject> searchObjects = os.searchObjects(getTransaction());
        assertEquals(testNodes.length, searchObjects.size());
        for (KomodoObject searchObject : searchObjects) {
            String name = searchObject.getName(getTransaction());
            assertTrue(name.startsWith("test"));

            Property property = searchObject.getProperty(getTransaction(), KomodoLexicon.VdbModel.MODEL_DEFINITION);
            assertEquals("DDL", property.getStringValue(getTransaction()));
        }
    }

    @Test
    public void executeFromQueryWithParentWhere() throws Exception {
        int sourceTotal = 5;
        assertNotNull(_repo);

        // Create the komodo workspace
        KomodoObject workspace = _repo.komodoWorkspace(getTransaction());
        assertNotNull(workspace);

        KomodoObject testModel1 = workspace.addChild(getTransaction(), "TestModel1", KomodoLexicon.VdbModel.NODE_TYPE);
        KomodoObject testModel2 = workspace.addChild(getTransaction(), "TestModel2", KomodoLexicon.VdbModel.NODE_TYPE);

        for (int i = 1; i <= sourceTotal; ++i) {
            testModel1.addChild(getTransaction(), "TestModelSource1-" + i, KomodoLexicon.VdbModelSource.NODE_TYPE);
        }

        KomodoObject[] testNodes = testModel1.getChildrenOfType(getTransaction(), KomodoLexicon.VdbModelSource.NODE_TYPE);
        assertEquals(sourceTotal, testNodes.length);

        for (int i = 1; i <= sourceTotal; ++i) {
            testModel2.addChild(getTransaction(), "TestModelSource2-" + i, KomodoLexicon.VdbModelSource.NODE_TYPE);
        }

        testNodes = testModel2.getChildrenOfType(getTransaction(), KomodoLexicon.VdbModelSource.NODE_TYPE);
        assertEquals(sourceTotal, testNodes.length);

        commit(); // must commit for search queries to work

        //
        // Test object searcher for immediate descendents beneath testModel1
        //
        ObjectSearcher os = new ObjectSearcher(_repo);
        os.addFromType(KomodoLexicon.VdbModelSource.NODE_TYPE);
        os.addWhereParentClause(null, null, testModel1.getAbsolutePath(), false);

        String expected = "SELECT [jcr:path], [mode:localName] FROM " +
                                     OPEN_SQUARE_BRACKET +
                                     KomodoLexicon.VdbModelSource.NODE_TYPE +
                                     CLOSE_SQUARE_BRACKET +
                                     " WHERE [" + JcrConstants.JCR_PATH + "] LIKE '" +
                                     testModel1.getAbsolutePath() + "/%'";
        assertEquals(expected, os.toString());

        List<KomodoObject> searchObjects = os.searchObjects(getTransaction());
        assertEquals(sourceTotal, searchObjects.size());

        for (KomodoObject searchObject : searchObjects) {
            String name = searchObject.getName(getTransaction());
            assertTrue(name.startsWith("TestModelSource1-"));
        }

        //
        // Find all the model source nodes beneath the workspace
        //
        os = new ObjectSearcher(_repo);
        os.addFromType(KomodoLexicon.VdbModelSource.NODE_TYPE);
        os.addWhereParentClause(null, null, workspace.getAbsolutePath(), false);
        searchObjects = os.searchObjects(getTransaction());
        assertEquals(sourceTotal * 2, searchObjects.size());
    }

    @Test
    public void executeFromQueryWithParentWhereDirectChildren() throws Exception {
        int sourceTotal = 5;
        assertNotNull(_repo);

        // Create the komodo workspace
        KomodoObject workspace = _repo.komodoWorkspace(getTransaction());
        assertNotNull(workspace);

        KomodoObject testModel1 = workspace.addChild(getTransaction(), "TestModel1", KomodoLexicon.VdbModel.NODE_TYPE);
        KomodoObject testModel2 = workspace.addChild(getTransaction(), "TestModel2", KomodoLexicon.VdbModel.NODE_TYPE);

        for (int i = 1; i <= sourceTotal; ++i) {
            KomodoObject child = testModel1.addChild(getTransaction(), "TestModelSource1-" + i, KomodoLexicon.VdbModelSource.NODE_TYPE);
            child.addChild(getTransaction(), "TestModelSource1-Child" + i, KomodoLexicon.VdbModelSource.NODE_TYPE);
        }

        KomodoObject[] testNodes = testModel1.getChildrenOfType(getTransaction(), KomodoLexicon.VdbModelSource.NODE_TYPE);
        assertEquals(sourceTotal, testNodes.length);

        for (int i = 1; i <= sourceTotal; ++i) {
            KomodoObject child = testModel2.addChild(getTransaction(), "TestModelSource2-" + i, KomodoLexicon.VdbModelSource.NODE_TYPE);
            child.addChild(getTransaction(), "TestModelSource2-Child" + i, KomodoLexicon.VdbModelSource.NODE_TYPE);
        }

        testNodes = testModel2.getChildrenOfType(getTransaction(), KomodoLexicon.VdbModelSource.NODE_TYPE);
        assertEquals(sourceTotal, testNodes.length);

        commit(); // must commit for search queries to work

        //
        // Test object searcher for immediate children beneath testModel1
        //
        ObjectSearcher os = new ObjectSearcher(_repo);
        os.addFromType(KomodoLexicon.VdbModelSource.NODE_TYPE);
        os.addWhereParentClause(null, null, testModel1.getAbsolutePath(), true);

        String expected = "SELECT [jcr:path], [mode:localName] FROM " +
                                     OPEN_SQUARE_BRACKET +
                                     KomodoLexicon.VdbModelSource.NODE_TYPE +
                                     CLOSE_SQUARE_BRACKET +
                                     " WHERE [" + JcrConstants.JCR_PATH + "] LIKE '" +
                                     testModel1.getAbsolutePath() + "/%'" +
                                     " AND ISCHILDNODE('" + testModel1.getAbsolutePath() + "')";
        assertEquals(expected, os.toString());
        System.out.println(expected);

        List<KomodoObject> searchObjects = os.searchObjects(getTransaction());

        // Only 5 children below testModel1
        assertEquals(5, searchObjects.size());

        for (KomodoObject searchObject : searchObjects) {
            String name = searchObject.getName(getTransaction());
            assertTrue(name.startsWith("TestModelSource1-"));
        }

        //
        // Test object searcher for all descendents beneath testModel1
        //
        os = new ObjectSearcher(_repo);
        os.addFromType(KomodoLexicon.VdbModelSource.NODE_TYPE);
        os.addWhereParentClause(null, null, testModel1.getAbsolutePath(), false);

        expected = "SELECT [jcr:path], [mode:localName] FROM " +
                                     OPEN_SQUARE_BRACKET +
                                     KomodoLexicon.VdbModelSource.NODE_TYPE +
                                     CLOSE_SQUARE_BRACKET +
                                     " WHERE [" + JcrConstants.JCR_PATH + "] LIKE '" +
                                     testModel1.getAbsolutePath() + "/%'";
        assertEquals(expected, os.toString());
        System.out.println(expected);

        searchObjects = os.searchObjects(getTransaction());

        // 5 children and 5 grandchildren below testModel1
        assertEquals(10, searchObjects.size());

        for (KomodoObject searchObject : searchObjects) {
            String name = searchObject.getName(getTransaction());
            System.out.println(searchObject.getAbsolutePath());
            assertTrue(name.startsWith("TestModelSource1-"));
        }
    }

    @Test
    public void executeFromQueryWithWhereContainsORExpression() throws Exception {
        assertNotNull(_repo);

        // Create the komodo workspace
        KomodoObject workspace = _repo.komodoWorkspace(getTransaction());
        assertNotNull(workspace);

        for (int i = 1; i < 6; ++i) {
            KomodoObject child = workspace.addChild(getTransaction(), "test" + i, KomodoLexicon.VdbModel.NODE_TYPE);
            child.setProperty(getTransaction(), KomodoLexicon.VdbModel.MODEL_DEFINITION, "DDL");
        }

        for (int i = 6; i < 11; ++i) {
            KomodoObject child = workspace.addChild(getTransaction(), "test" + i, KomodoLexicon.VdbModel.NODE_TYPE);
            child.setProperty(getTransaction(), KomodoLexicon.VdbModel.MODEL_DEFINITION, "TEIIDSQL");
        }

        KomodoObject[] testNodes = workspace.getChildrenOfType(getTransaction(), KomodoLexicon.VdbModel.NODE_TYPE);
        assertEquals(10, testNodes.length);
        for (KomodoObject testKO : testNodes) {
            Property property = testKO.getProperty(getTransaction(), KomodoLexicon.VdbModel.MODEL_DEFINITION);
            assertTrue(property.getStringValue(getTransaction()).equals("DDL") || property.getStringValue(getTransaction()).equals("TEIIDSQL"));
        }

        ObjectSearcher os = new ObjectSearcher(_repo);
        os.addFromType(JcrConstants.NT_UNSTRUCTURED, "nt");
        os.addWhereContainsClause(null, "nt",
                                                     KomodoLexicon.VdbModel.MODEL_DEFINITION,
                                                     KeywordCriteria.ANY,
                                                     "DDL", "TEIIDSQL");

        String expected = "SELECT [jcr:path], [mode:localName] FROM [nt:unstructured] AS nt " +
                                     "WHERE CONTAINS(nt.[vdb:modelDefinition], 'DDL OR TEIIDSQL')";
        assertEquals(expected, os.toString());

        commit(); // must commit for search queries to work

        List<KomodoObject> searchObjects = os.searchObjects(getTransaction());
        assertEquals(testNodes.length, searchObjects.size());
        for (KomodoObject searchObject : searchObjects) {
            String name = searchObject.getName(getTransaction());
            assertTrue(name.startsWith("test"));

            String indexStr = name.substring(4);
            int index = Integer.parseInt(indexStr);
            assertTrue(index > 0 && index < 11);

            Property property = searchObject.getProperty(getTransaction(), KomodoLexicon.VdbModel.MODEL_DEFINITION);
            if (index < 6)
                assertEquals("DDL", property.getStringValue(getTransaction()));
            else
                assertEquals("TEIIDSQL", property.getStringValue(getTransaction()));
        }
    }

    @Test
    public void executePathQueryToFindWorkspace() throws Exception {
        assertNotNull(_repo);

        // Create the komodo workspace
        KomodoObject workspace = _repo.komodoWorkspace(getTransaction());
        assertNotNull(workspace);

        System.out.println(RepositoryTools.traverse(getTransaction(), workspace.getParent(getTransaction())));

        commit(); // must commit for search queries to work

        ObjectSearcher os = new ObjectSearcher(_repo);
        os.addFromType(JcrConstants.NT_UNSTRUCTURED, "nt");
        os.addWherePathClause(null, "nt", RepositoryImpl.WORKSPACE_ROOT);

        String expected = "SELECT [jcr:path], [mode:localName] FROM [nt:unstructured] AS nt " +
                                     "WHERE PATH(nt) = '" + workspace.getAbsolutePath() + "'";
        assertEquals(expected, os.toString());

        List<KomodoObject> searchObjects = os.searchObjects(getTransaction());
        assertEquals(1, searchObjects.size());

        assertEquals(workspace.getAbsolutePath(), searchObjects.iterator().next().getAbsolutePath());
    }
}
