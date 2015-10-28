package org.komodo.shell;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.komodo.shell.commands.AddChildCommandTest;
import org.komodo.shell.commands.CdCommandTest;
import org.komodo.shell.commands.CommitCommandTest;
import org.komodo.shell.commands.DeleteChildCommandTest;
import org.komodo.shell.commands.ExitCommandTest;
import org.komodo.shell.commands.HelpCommandTest;
import org.komodo.shell.commands.HomeCommandTest;
import org.komodo.shell.commands.LibraryCommandTest;
import org.komodo.shell.commands.ListCommandTest;
import org.komodo.shell.commands.PlayCommandTest;
import org.komodo.shell.commands.RenameCommandTest;
import org.komodo.shell.commands.RollbackCommandTest;
import org.komodo.shell.commands.SetAutoCommitCommandTest;
import org.komodo.shell.commands.SetGlobalPropertyCommandTest;
import org.komodo.shell.commands.SetPropertyCommandTest;
import org.komodo.shell.commands.SetRecordCommandTest;
import org.komodo.shell.commands.ShowChildrenCommandTest;
import org.komodo.shell.commands.ShowGlobalCommandTest;
import org.komodo.shell.commands.ShowPropertiesCommandTest;
import org.komodo.shell.commands.ShowPropertyCommandTest;
import org.komodo.shell.commands.ShowStatusCommandTest;
import org.komodo.shell.commands.ShowSummaryCommandTest;
import org.komodo.shell.commands.UnsetPropertyCommandTest;
import org.komodo.shell.commands.WorkspaceCommandTest;

/**
 * All Tests for CommandLine
 */
@RunWith(Suite.class)
@Suite.SuiteClasses( { AddChildCommandTest.class,
                       CdCommandTest.class, 
                       CommitCommandTest.class,
                       DeleteChildCommandTest.class,
                       ExitCommandTest.class, 
                       HelpCommandTest.class,
                       HomeCommandTest.class, 
                       LibraryCommandTest.class, 
                       ListCommandTest.class, 
                       PlayCommandTest.class,
                       RenameCommandTest.class, 
                       RollbackCommandTest.class, 
                       SetAutoCommitCommandTest.class,
                       SetGlobalPropertyCommandTest.class, 
                       SetPropertyCommandTest.class, 
                       SetRecordCommandTest.class,
                       ShowChildrenCommandTest.class, 
                       ShowGlobalCommandTest.class, 
                       ShowPropertiesCommandTest.class,
                       ShowPropertyCommandTest.class, 
                       ShowStatusCommandTest.class, 
                       ShowSummaryCommandTest.class,
                       UnsetPropertyCommandTest.class, 
                       WorkspaceCommandTest.class } )
public class AllTests {
    // nothing to do
}
