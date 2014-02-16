package com.adviser.osgi.db.datasource;

import java.sql.SQLException;
import java.util.Dictionary;

public interface DataSourceFactory {

    public final static String PROP_CREATE_DATABASE_NAME = "createDatabaseName";
    public final static String PROP_CREATE_DATABASE_DATASOURCE_NAME = "createDatabaseDataSourceName";


    /**
     * Create a datasource based on the properties provided. The datasource will be registered as IPojo service.
     * 
     * @param props
     *            dictionary containing at least the following properties: <tt>driver, name, service.pid, url, user, password</tt>
     * @return PID of the service
     */
    String createDataSource(Dictionary<String, String> props);

    /**
     * Delete a datasource instance identified by the PID. The datasource will be closed and the IPojo instance will be disposed.
     * 
     * @return PID of the datasource if successfully created, null otherwise
     */
    void deleteDataSource(String pid);

    /**
     * Create a database based on the properties provided. The database will be created using an existing "system datasource" using
     * SQL "create database" command. After creation of the database on the database server the DataSourceFactory.createDataSource()
     * method will be called to create a datasource and IPojo instance.
     * <p/>
     * The system datasource must be available before using this method. The name of the system datasource has to be provided as
     * property <tt>createDatabaseDataSourceName</tt>. The name of the database to be created has to be provided as property
     * <tt>createDatabaseName</tt>.
     * 
     * @param props
     *            dictionary containing at least the following properties: <tt>driver, name, service.pid, url, user, password, 
     *            createDatabaseDataSourceName, createDatabaseName</tt>
     * @return PID of the datasource if successfully created, null otherwise
     * @throws SQLException
     *             in case of an error
     */

    String createDatabaseFromName(Dictionary<String, String> props) throws SQLException;

    /**
     * Drop a database based on the properties provided. Similar to the createDatabaseFromName() method the correct properties must
     * be provided.
     * 
     * @param props
     *            dictionary containing at least the following properties: <tt>driver, name, service.pid, url, user, password, 
     *            createDatabaseDataSourceName, createDatabaseName</tt>
     * @throws SQLException
     *             in case of an error
     */
    void dropDatabase(Dictionary<String, String> props) throws SQLException;

    /**
     * Check if a database exists based on the properties provided. Similar to the createDatabaseFromName() method the correct
     * properties must be provided.
     * 
     * @param props
     *            dictionary containing at least the following properties: <tt>driver, name, service.pid, url, user, password, 
     *            createDatabaseDataSourceName, createDatabaseName</tt>
     * @returntrue, if database already exists, false otherwise
     * @throws SQLException
     */
    boolean existsDatabase(final Dictionary<String, String> props) throws SQLException;

}
