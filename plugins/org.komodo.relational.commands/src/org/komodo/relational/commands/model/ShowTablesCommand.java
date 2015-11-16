/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.komodo.relational.commands.model;

import static org.komodo.relational.commands.model.ModelCommandMessages.ShowTablesCommand.NO_TABLES;
import static org.komodo.relational.commands.model.ModelCommandMessages.ShowTablesCommand.TABLES_HEADER;
import static org.komodo.relational.commands.workspace.WorkspaceCommandMessages.General.PRINT_RELATIONAL_OBJECT;
import static org.komodo.shell.CompletionConstants.MESSAGE_INDENT;
import org.komodo.relational.model.Model;
import org.komodo.relational.model.Table;
import org.komodo.shell.CommandResultImpl;
import org.komodo.shell.api.CommandResult;
import org.komodo.shell.api.WorkspaceStatus;

/**
 * A shell command to show all the {@link Table tables} of a {@link Model model}.
 */
public final class ShowTablesCommand extends ModelShellCommand {

    static final String NAME = "show-tables"; //$NON-NLS-1$

    /**
     * @param status
     *        the shell's workspace status (cannot be <code>null</code>)
     */
    public ShowTablesCommand( final WorkspaceStatus status ) {
        super( NAME, status );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.shell.BuiltInShellCommand#doExecute()
     */
    @Override
    protected CommandResult doExecute() {
        try {
            final Model model = getModel();
            final Table[] tables = model.getTables( getTransaction() );

            if ( tables.length == 0 ) {
                print( MESSAGE_INDENT, getMessage( NO_TABLES, model.getName( getTransaction() ) ) );
            } else {
                print( MESSAGE_INDENT, getMessage( TABLES_HEADER, model.getName( getTransaction() ) ) );

                final int indent = (MESSAGE_INDENT * 2);

                for ( final Table table : tables ) {
                    print( indent,
                           getWorkspaceMessage( PRINT_RELATIONAL_OBJECT,
                                                table.getName( getTransaction() ),
                                                table.getTypeDisplayName() ) );
                }
            }

            return CommandResult.SUCCESS;
        } catch ( final Exception e ) {
            return new CommandResultImpl( e );
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.shell.BuiltInShellCommand#getMaxArgCount()
     */
    @Override
    protected int getMaxArgCount() {
        return 0;
    }

}