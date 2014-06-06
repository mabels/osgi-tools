package com.adviser.osgi.db.datasource.base;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.KeyedObjectPoolFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adviser.osgi.db.datasource.DriverFactory;
import com.adviser.osgi.helper.IPOJOInstanceHelper;

@Component
@Instantiate
@Provides
public class DataSourceFactory implements com.adviser.osgi.db.datasource.DataSourceFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceFactory.class);

    private static final Map<String, DataSourceParams> pid2dataSourceParams = new HashMap<String, DataSourceParams>();

    private final List<DriverAndServiceReference> drivers = new LinkedList<DriverAndServiceReference>();

    private final IPOJOInstanceHelper ipojoInstanceHelper;


    /** {@inheritDoc} */
    @Override
    public String createDataSource(final Dictionary<String, String> props) {
        final String pid = props.get(Constants.SERVICE_PID);
        if (pid == null) {
            LOGGER.error("DataSourceFactory:createDataSource: property '" + Constants.SERVICE_PID + "' is not set");
            return null;
        }
        if (props.get("driver") == null) {
            LOGGER.error("DataSourceFactory:createDataSource: property 'driver' is not set");
            return null;
        }

        final DataSourceParams dataSourceParams = new DataSourceParams(props);
        pid2dataSourceParams.put(pid, dataSourceParams);

        final String driverName = props.get("driver");
        final DriverAndServiceReference dasr = findByName(driverName);
        if (dasr == null) {
            LOGGER.warn("createDataSource:{} no driver found with name {}, will be retried later",
                    props.get(Constants.SERVICE_PID), driverName);
            return null;
        }

        LOGGER.info("DriverAndServiceReference:{}=>{}", driverName, dasr);

        for (final String key : dasr.serviceReference.getPropertyKeys()) {
            LOGGER.info("ServiceReference:driver.{}=>{}", key, dasr.serviceReference.getProperty(key));
            props.put("driver." + key, dasr.serviceReference.getProperty(key).toString());
        }

        LOGGER.info("DataSourceFactory:createOrUpdateDataSource:{}:{}", pid, driverName);

        create(dasr.driverFactory.get(), props, dataSourceParams);

        props.put("osgi.jndi.service.name", getString("name", props));
        props.put(Constants.SERVICE_PID, pid);
        props.put("instance.name", getString("name", props));
        final Enumeration<String> keys = props.keys();
        final Dictionary<String, Object> soProps = new Hashtable<String, Object>();
        while (keys.hasMoreElements()) {
            final String key = keys.nextElement();
            soProps.put(key, props.get(key));
            LOGGER.info("{}={}", key, props.get(key));
        }
        soProps.put("dataSourceParams", dataSourceParams);
        dataSourceParams.instanceHolder = this.ipojoInstanceHelper.create(DataSource.class, IPojoPoolingDataSource.class.getName(),
                pid, soProps);

        return pid;
    }

    /** {@inheritDoc} */
    @Override
    public void deleteDataSource(final String pid) {
        final DataSourceParams dsp = pid2dataSourceParams.get(pid);
        if (dsp != null) {
            LOGGER.info("DataSourceFactory:deleteDataSource:{}", pid);
            try {
                if (dsp.instanceHolder != null) {
                    dsp.instanceHolder.dispose();
                }
                dsp.poolableConnectionFactory.getPool().close();
            } catch (final Exception ex) {
                LOGGER.error("deleteDataSource:{} {}", pid, ex);
            }
            pid2dataSourceParams.remove(pid);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String createDatabaseFromName(final Dictionary<String, String> props) throws SQLException {
        final DataSource systemds = getSystemDS(props);
        if (systemds == null) {
            LOGGER.error("Could not drop database {}. No system datasource found to drop database for PID.",
                    props.get(PROP_CREATE_DATABASE_NAME));
            return null;
        }
        runStmt(systemds, "CREATE DATABASE " + props.get(PROP_CREATE_DATABASE_NAME), props.get(PROP_CREATE_DATABASE_NAME));

        LOGGER.info("Successfully created database for PID {}.", props.get(PROP_CREATE_DATABASE_NAME));

        return createDataSource(props);
    }

    /** {@inheritDoc} */
    @Override
    public void dropDatabase(final Dictionary<String, String> props) throws SQLException {
        final String pid = props.get(Constants.SERVICE_PID);
        DataSourceParams param = pid2dataSourceParams.get(pid);
        if (param == null) {
            // for safety create a database instance first before dropping it
            if (createDataSource(props) == null) {
                LOGGER.error("Could not drop database {}. The datasource could not be created.", pid);
                return;
            }
        }
        // now the datasource should be in the pid map
        param = pid2dataSourceParams.get(pid);
        if (param != null) {
            // datasource for PID found, delete the datasource instance
            deleteDataSource(pid);
        } else {
            LOGGER.error("Could not drop database {}. The datasource could not be created.", pid);
            return;
        }

        final DataSource systemds = getSystemDS(props);
        if (systemds == null) {
            LOGGER.error("Could not drop database {}. No system datasource found to drop database for PID.", pid);
            return;
        }
        runStmt(systemds, "DROP DATABASE " + props.get(PROP_CREATE_DATABASE_NAME), props.get(PROP_CREATE_DATABASE_NAME));

        LOGGER.info("Successfully dropped database for PID {}.", pid);
    }

    /** {@inheritDoc} */
    @Override
    public boolean existsDatabase(final Dictionary<String, String> props) throws SQLException {
        final DataSource systemds = getSystemDS(props);
        if (systemds == null) {
            LOGGER.error("Could not check database existence for {}. No system datasource found to drop database for PID.",
                    props.get(PROP_CREATE_DATABASE_NAME));
            return false;
        }
        String sql = "select * from pg_database where datname = '" + props.get(PROP_CREATE_DATABASE_NAME) + "'";
        boolean result = false;
        final Connection connection = systemds.getConnection();
        try {
            final Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(sql);
            result = (rs != null && rs.next());
            st.close();
        } finally {
            connection.close();
        }
        return result;
    }

    private DataSource findDataSourceByName(final String dname) {
        for (final DataSourceParams dsp : pid2dataSourceParams.values()) {
            if (dname != null && dname.equals(dsp.props.get("name"))) {
                return dsp.poolingDataSource;
            }
        }
        return null;
    }

    private DataSource getSystemDS(final Dictionary<String, String> props) {
        final String databaseName = props.get(PROP_CREATE_DATABASE_NAME);
        final String systemDataSourceName = props.get(PROP_CREATE_DATABASE_DATASOURCE_NAME);
        if (databaseName == null || databaseName.isEmpty() || systemDataSourceName == null || systemDataSourceName.isEmpty()) {
            LOGGER.error("Properties must contain key:{} and key:{}", PROP_CREATE_DATABASE_NAME,
                    PROP_CREATE_DATABASE_DATASOURCE_NAME);
            return null;
        }
        final DataSource systemds = findDataSourceByName(systemDataSourceName);
        if (systemds == null) {
            LOGGER.error("Can't find datasource from name {}", databaseName);
            return null;
        }
        return systemds;
    }

    private void runStmt(final DataSource ds, final String sql, final String... params) throws SQLException {
        final Connection connection = ds.getConnection();
        try {
            connection.setAutoCommit(true);
            final Statement ps = connection.createStatement();
            ps.execute(sql);
            ps.close();
        } finally {
            connection.close();
        }
    }

    public DataSourceFactory(final BundleContext context) {
        this.ipojoInstanceHelper = new IPOJOInstanceHelper(context);
    }

    @Validate
    public void start() {
        LOGGER.info("DataSourceFactory:start");
    }

    @Invalidate
    public void stop() {
        LOGGER.info("DataSourceFactory:stop");
    }

    @Bind(optional = true, aggregate = true)
    public void addDriver(final DriverFactory driver, final ServiceReference<DriverFactory> sr) {
        String driverClassName = driver.getClass().getCanonicalName();
        final String driverSignature = String.format("%s-%s-%s", driverClassName, sr.getProperty("name"),
                sr.getProperty("fullName"));

        LOGGER.info("DataSourceFactory:addDriver:{}", driverClassName);

        this.drivers.add(new DriverAndServiceReference(driver, sr));
        // Replay Connection
        for (final Map.Entry<String, DataSourceParams> entry : pid2dataSourceParams.entrySet()) {
            if (isDriver(entry.getValue().props.get("driver"), sr)) {
                if (entry.getValue().serviceRegistration != null) {
                    LOGGER.error("DataSource {} already registered", driverSignature);
                    continue;
                }
                createDataSource(entry.getValue().props);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private boolean isDriver(final String name, final ServiceReference sr) {
        final String srName = (String) sr.getProperty("name");
        if (srName != null && srName.equals(name)) {
            return true;
        }
        final String srFullName = (String) sr.getProperty("fullName");
        if (srFullName != null && srFullName.equals(name)) {
            return true;
        }
        return false;
    }

    @SuppressWarnings({ "unused", "rawtypes" })
    private boolean isDriver(final Dictionary<String, String> prop, final ServiceReference sr2) {
        return isDriver(prop.get("name"), sr2) || isDriver(prop.get("fullName"), sr2);
    }

    @SuppressWarnings("rawtypes")
    private boolean isDriver(final ServiceReference sr1, final ServiceReference sr2) {
        return isDriver(sr1.getProperty("name").toString(), sr2) || isDriver(sr1.getProperty("fullName").toString(), sr2);
    }

    private DriverAndServiceReference findByName(final String name) {
        for (final DriverAndServiceReference driver : this.drivers) {
            ServiceReference<DriverFactory> serviceRef = driver.serviceReference;
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("findByName:{}=={}||{}", name, serviceRef.getProperty("name"), serviceRef.getProperty("fullName"));
            }
            if (isDriver(name, serviceRef)) {
                return driver;
            }
        }
        return null;
    }

    @Unbind(optional = true, aggregate = true)
    public void delDriver(final DriverFactory driver, final ServiceReference<DriverFactory> sr) {
        try {
            final String name = sr.getProperty("name").toString();
            final String fullName = sr.getProperty("fullName").toString();

            LOGGER.info("DataSourceFactory:delDriver:{}", driver.getClass().getCanonicalName());

            this.drivers.remove(findByName(name));
            this.drivers.remove(findByName(fullName));
            final List<DataSourceParams> toRenew = new LinkedList<DataSourceParams>();

            for (final Map.Entry<String, DataSourceParams> entry : pid2dataSourceParams.entrySet()) {
                if (isDriver(sr, entry.getValue().serviceRegistration.getReference())) {
                    toRenew.add(entry.getValue());
                }
            }
            for (final DataSourceParams dsp : toRenew) {
                deleteDataSource(dsp.props.get(Constants.SERVICE_PID));
                dsp.serviceRegistration = null;
                pid2dataSourceParams.put(dsp.props.get(Constants.SERVICE_PID), dsp);
            }
        } catch (final Exception e) {
            LOGGER.error("Error while deleting JDBC driver: ", e.getMessage());
        }
    }

    protected static String getString(final String key, final Dictionary<?, ?> properties) {
        final Object value = properties.get(key);
        return (value instanceof String) ? (String) value : null;
    }

    protected static int getInt(String key, Dictionary<?, ?> dict, int def) {
        Object val = dict.get(key);
        if (val == null) {
            return def;
        }
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            LOGGER.warn("Could not parse {} as integer. Returning passed default {}.", val, def);
            return def;
        }
    }

    protected static long getLong(String key, Dictionary<?, ?> dict, long def) {
        Object val = dict.get(key);
        if (val == null) {
            return def;
        }
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            LOGGER.warn("Could not parse {} as long. Returning passed default {}.", val, def);
            return def;
        }
    }

    protected static boolean getBool(String key, Dictionary<?, ?> dict, boolean def) {
        Object val = dict.get(key);
        if (val == null) {
            return def;
        }
        return Boolean.parseBoolean(val.toString());
    }

    private static DataSourceParams create(final Driver driver, final Dictionary<String, String> dict, final DataSourceParams dsp) {
        final Properties driverProp = new Properties();
        final Enumeration<String> keys = dict.keys();
        while (keys.hasMoreElements()) {
            final String key = keys.nextElement();
            driverProp.setProperty(key, dict.get(key));
        }
        final String uri = getString("url", dict);
        final int jdbcIdx = uri.indexOf(':');
        if (jdbcIdx <= 0) {
            LOGGER.error("url not parsable:{}", uri);
            return null;
        }
        final int uriIdx = uri.indexOf(':', jdbcIdx + 1);
        if (uriIdx <= 0) {
            LOGGER.error("url not parsable:{}", uri);
            return null;
        }
        LOGGER.info("DataSource:createDriver:uri={}", uri);
        final ConnectionFactory connectionFactory = new ConnectionFactory() {

            @Override
            public Connection createConnection() throws SQLException {
                final Connection connection = driver.connect(uri, driverProp);
                LOGGER.info("ConnectionFactory:createConnection:{}:{}:{}", driver.getClass().getName(), uri, connection);
                return connection;
            }
        };

        final GenericObjectPool connectionPool = new GenericObjectPool();
        final GenericObjectPool.Config poolConfig = createPoolConfig(dict);
        connectionPool.setConfig(poolConfig);

        final KeyedObjectPoolFactory stmtPoolFactory = null; // new GenericKeyedObjectPoolFactory(null, stmtConfig)
        final String validationQuery = getString("validationQuery", dict); // getString("driver.sql.validation.query", dict)
        final boolean defaultReadOnly = false;
        final boolean defaultAutoCommit = false;

        dsp.poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, connectionPool, stmtPoolFactory,
                validationQuery, defaultReadOnly, defaultAutoCommit);
        dsp.poolingDataSource = new PoolingDataSource(dsp.poolableConnectionFactory.getPool());

        return dsp;
    }

    private static GenericObjectPool.Config createPoolConfig(final Dictionary<String, String> dict) {
        final GenericObjectPool.Config conf = new GenericObjectPool.Config();
        // stmtConfig.lifo = true;

        conf.maxIdle = getInt("maxIdle", dict, GenericObjectPool.DEFAULT_MAX_IDLE);
        conf.maxActive = getInt("maxActive", dict, 30);
        conf.minIdle = getInt("minIdle", dict, GenericObjectPool.DEFAULT_MIN_IDLE);
        conf.maxWait = getLong("maxWait", dict, GenericObjectPool.DEFAULT_MAX_WAIT);

        // public byte whenExhaustedAction = GenericObjectPool.DEFAULT_WHEN_EXHAUSTED_ACTION;
        conf.whenExhaustedAction = (byte) getInt("whenExhaustedAction", dict, GenericObjectPool.WHEN_EXHAUSTED_FAIL);

        conf.testOnBorrow = getBool("testOnBorrow", dict, GenericObjectPool.DEFAULT_TEST_ON_BORROW);
        conf.testOnReturn = getBool("testOnReturn", dict, GenericObjectPool.DEFAULT_TEST_ON_RETURN);
        conf.testWhileIdle = getBool("testWhileIdle", dict, GenericObjectPool.DEFAULT_TEST_WHILE_IDLE);

        // public long timeBetweenEvictionRunsMillis = GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;
        conf.timeBetweenEvictionRunsMillis = getLong("timeBetweenEvictionRunsMillis", dict, 500L);

        conf.numTestsPerEvictionRun = getInt("numTestsPerEvictionRun", dict, GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN);
        conf.minEvictableIdleTimeMillis = getLong("minEvictableIdleTimeMillis", dict,
                GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS);

        return conf;
    }

}
