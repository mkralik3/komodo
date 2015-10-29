/*
* JBoss, Home of Professional Open Source.
*
* See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
*
* See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
*/
package org.komodo.rest.relational.json;

import org.komodo.rest.KomodoRestEntity;
import org.komodo.rest.KomodoRestProperty;
import org.komodo.rest.KomodoStatusObject;
import org.komodo.rest.RestLink;
import org.komodo.rest.json.LinkSerializer;
import org.komodo.rest.relational.RestVdb;
import org.komodo.rest.relational.RestVdbCondition;
import org.komodo.rest.relational.RestVdbDataRole;
import org.komodo.rest.relational.RestVdbImport;
import org.komodo.rest.relational.RestVdbMask;
import org.komodo.rest.relational.RestVdbModel;
import org.komodo.rest.relational.RestVdbModelSource;
import org.komodo.rest.relational.RestVdbPermission;
import org.komodo.rest.relational.RestVdbTranslator;
import org.komodo.utils.ArgCheck;
import org.komodo.utils.KLog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * A JSON serializer and deserializer for {@link KomodoRestEntity Komodo REST objects}.
 */
public final class KomodoJsonMarshaller {

    private static final KLog LOGGER = KLog.getLogger();

    /**
     * The shared JSON serialier/deserializer for {@link KomodoRestEntity} objects.
     */
    protected static final Gson BUILDER;

    protected static final Gson PRETTY_BUILDER;

    static {
        final GsonBuilder temp = new GsonBuilder().registerTypeAdapter( RestLink.class, new LinkSerializer() )
                                                  .registerTypeAdapter(KomodoStatusObject.class, new StatusObjectSerializer())
                                                  .registerTypeAdapter( KomodoRestProperty.class, new KomodoRestPropertySerializer() )
                                                  .registerTypeAdapter( RestVdb.class, new VdbSerializer() )
                                                  .registerTypeAdapter( RestVdbModel.class, new VdbModelSerializer() )
                                                  .registerTypeAdapter( RestVdbModelSource.class, new VdbModelSourceSerializer() )
                                                  .registerTypeAdapter( RestVdbDataRole.class, new VdbDataRoleSerializer() )
                                                  .registerTypeAdapter( RestVdbImport.class, new VdbImportSerializer() )
                                                  .registerTypeAdapter( RestVdbPermission.class, new VdbPermissionSerializer() )
                                                  .registerTypeAdapter( RestVdbCondition.class, new VdbConditionSerializer() )
                                                  .registerTypeAdapter( RestVdbMask.class, new VdbMaskSerializer() )
                                                  .registerTypeAdapter( RestVdbTranslator.class, new VdbTranslatorSerializer() );
        BUILDER = temp.create();
        PRETTY_BUILDER = temp.setPrettyPrinting().create();
    }

    /**
     * Outputs a non-pretty printed JSON representation.
     *
     * @param entity
     *        the entity whose JSON representation is being requested (cannot be <code>null</code>)
     * @return the JSON representation (never empty)
     */
    public static String marshall( final KomodoRestEntity entity ) {
        return marshall( entity, true );
    }

    /**
     * @param entity
     *        the entity whose JSON representation is being requested (cannot be <code>null</code>)
     * @param prettyPrint
     *        <code>true</code> if JSON output should be pretty printed
     * @return the JSON representation (never empty)
     */
    public static String marshall( final KomodoRestEntity entity,
                                   final boolean prettyPrint ) {
        ArgCheck.isNotNull( entity, "entity" ); //$NON-NLS-1$

        String json = null;

        if ( prettyPrint ) {
            json = PRETTY_BUILDER.toJson( entity );
        } else {
            json = BUILDER.toJson( entity );
        }

        LOGGER.debug( "marshall: {0}", json ); //$NON-NLS-1$
        return json;
    }

    /**
     * @param entities
     *        the entities whose JSON representation is being requested (cannot be <code>null</code>)
     * @param prettyPrint
     *        <code>true</code> if JSON output should be pretty printed
     * @return the JSON representation (never empty)
     */
    public static String marshallArray( final KomodoRestEntity[] entities,
                                   final boolean prettyPrint ) {
        ArgCheck.isNotNull( entities, "entities" ); //$NON-NLS-1$

        String json = null;

        if ( prettyPrint ) {
            json = PRETTY_BUILDER.toJson( entities );
        } else {
            json = BUILDER.toJson( entities );
        }

        LOGGER.debug( "marshall: {0}", json ); //$NON-NLS-1$
        return json;
    }

    /**
     * @param status
     *        the status object whose JSON representation is being requested (cannot be <code>null</code>)
     * @param prettyPrint
     *        <code>true</code> if JSON output should be pretty printed
     * @return the JSON representation (never empty)
     */
    public static String marshall(KomodoStatusObject status, boolean prettyPrint) {
        ArgCheck.isNotNull( status, "status" ); //$NON-NLS-1$

        String json = null;

        if ( prettyPrint ) {
            json = PRETTY_BUILDER.toJson(status);
        } else {
            json = BUILDER.toJson(status);
        }

        LOGGER.debug( "marshall: {0}", json ); //$NON-NLS-1$
        return json;

    }

    /**
     * @param <T>
     *        the {@link KomodoRestEntity} type of the output
     * @param json
     *        the JSON representation being converted to a {@link KomodoRestEntity} (cannot be empty)
     * @param entityClass
     *        the type of {@link KomodoRestEntity} the JSON will be converted to (cannot be <code>null</code>)
     * @return the {@link KomodoRestEntity} (never <code>null</code>)
     */
    public static < T extends KomodoRestEntity > T unmarshall( final String json,
                                                               final Class< T > entityClass ) {
        final T entity = BUILDER.fromJson( json, entityClass );
        LOGGER.debug( "unmarshall: class = {0}, entity = {1}", entityClass, entity ); //$NON-NLS-1$
        return entity;
    }

    /**
     * @param <T>
     *        the {@link KomodoRestEntity} type of the output
     * @param json
     *        the JSON representation being converted to a {@link KomodoRestEntity} (cannot be empty)
     * @param entityClass
     *        the type of {@link KomodoRestEntity} the JSON will be converted to (cannot be <code>null</code>)
     * @return the {@link KomodoRestEntity} (never <code>null</code>)
     */
    public static < T extends KomodoRestEntity > T[] unmarshallArray( final String json,
                                                               final Class< T[] > entityClass ) {
        final T[] entity = BUILDER.fromJson( json, entityClass );
        LOGGER.debug( "unmarshall: class = {0}, entity = {1}", entityClass, entity ); //$NON-NLS-1$
        return entity;
    }

    /**
     * @param json
     *        the JSON representation of a {@link KomodoStatusObject} (cannot be empty)
     * @return the {@link KomodoStatusObject} (never <code>null</code>)
     */
    public static KomodoStatusObject unmarshallKSO( final String json) {
        final KomodoStatusObject entity = BUILDER.fromJson( json, KomodoStatusObject.class);
        LOGGER.debug( "unmarshall: class = {0}, entity = {1}", KomodoStatusObject.class, entity ); //$NON-NLS-1$
        return entity;
    }

    /**
     * Don't allow construction outside of this class.
     */
    private KomodoJsonMarshaller() {
        // nothing to do
    }

}