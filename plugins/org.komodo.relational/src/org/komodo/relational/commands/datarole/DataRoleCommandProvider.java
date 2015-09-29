/*
 * JBoss, Home of Professional Open Source.
*
* See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
*
* See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
*/
package org.komodo.relational.commands.datarole;

import java.util.HashMap;
import java.util.Map;
import org.komodo.relational.vdb.DataRole;
import org.komodo.relational.vdb.internal.DataRoleImpl;
import org.komodo.shell.api.ShellCommand;
import org.komodo.shell.api.ShellCommandProvider;
import org.komodo.shell.api.WorkspaceStatus;
import org.komodo.spi.KException;
import org.komodo.spi.repository.KomodoObject;
import org.komodo.spi.repository.Repository;

/**
 * A shell command provider for {@link DataRole}s.
 */
public class DataRoleCommandProvider implements ShellCommandProvider {

    /**
     * Constructs a command provider for {@link DataRole} shell commands.
     */
    public DataRoleCommandProvider() {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.shell.api.ShellCommandProvider#provideCommands()
     */
    @Override
    public Map< String, Class< ? extends ShellCommand >> provideCommands() {
        final Map< String, Class< ? extends ShellCommand >> result = new HashMap<>();

        result.put( AddMappedRoleCommand.NAME, AddMappedRoleCommand.class );
        result.put( AddPermissionCommand.NAME, AddPermissionCommand.class );
        result.put( DeleteMappedRoleCommand.NAME, DeleteMappedRoleCommand.class );
        result.put( DeletePermissionCommand.NAME, DeletePermissionCommand.class );
        result.put( SetDataRolePropertyCommand.NAME, SetDataRolePropertyCommand.class );

        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.shell.api.ShellCommandProvider#resolve(org.komodo.spi.repository.Repository.UnitOfWork,
     *      org.komodo.spi.repository.KomodoObject)
     */
    @SuppressWarnings( "unchecked" )
    @Override
    public DataRole resolve ( final Repository.UnitOfWork uow, final KomodoObject kObj ) throws KException {
        if(DataRoleImpl.RESOLVER.resolvable(uow, kObj)) {
            return DataRoleImpl.RESOLVER.resolve(uow, kObj);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.shell.api.ShellCommandProvider#isRoot(org.komodo.spi.repository.Repository.UnitOfWork,
     *      org.komodo.spi.repository.KomodoObject)
     */
    @Override
    public boolean isRoot ( final Repository.UnitOfWork uow, final KomodoObject kObj ) throws KException {
        if(DataRoleImpl.RESOLVER.resolvable(uow, kObj)) {
            return false;
        }
        return false;
    }

    @Override
    public String getTypeDisplay ( final Repository.UnitOfWork uow, final KomodoObject kObj ) throws KException {
        if(DataRoleImpl.RESOLVER.resolvable(uow, kObj)) {
            DataRole dataRole = DataRoleImpl.RESOLVER.resolve(uow, kObj);
            return dataRole.getTypeDisplayName();
        }
        return null;
    }

    /**
     * @throws KException the exception
     */
    @Override
    public String getStatusMessage ( final Repository.UnitOfWork uow, final KomodoObject kObj ) throws KException {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.shell.api.ShellCommandProvider#initWorkspaceState(org.komodo.shell.api.WorkspaceStatus)
     */
    @Override
    public void initWorkspaceState(WorkspaceStatus wsStatus) {
        // Init any workspace state
    }

}
