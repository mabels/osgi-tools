package de.nextaudience.db.datasource.base;

import de.nextaudience.db.datasource.DriverFactory;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
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
import org.apache.commons.dbcp.DriverConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.KeyedObjectPoolFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPoolFactory;
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
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Instantiate
@Provides
public class DataSourceFactory implements de.nextaudience.db.datasource.DataSourceFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceFactory.class);

    private static final Map<String, DataSourceParams> pid2dataSourceParams = new HashMap<String, DataSourceParams>();

    public String createDataSource(Dictionary<String, String> props) {
        final String pid = props.get(Constants.SERVICE_PID);
        if (pid == null) {
            LOGGER.error("DataSourceFactory:createDataSource: property '" + Constants.SERVICE_PID + "' is not set");
        }
        if (props.get("driver") == null) {
            LOGGER.error("DataSourceFactory:createDataSource: property 'driver' is not set");
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
        for (String key : dasr.serviceReference.getPropertyKeys()) {
            LOGGER.info("ServiceReference:driver.{}=>{}", key, dasr.serviceReference.getProperty(key));
            props.put("driver."+key, dasr.serviceReference.getProperty(key).toString());
        }
        LOGGER.info("DataSourceFactory:createOrUpdateDataSource:{}:{}", pid, driverName);
        final DataSource dataSource = create(dasr.driverFactory.get(), props);
        props.put("osgi.jndi.service.name", getString("name", props));
        props.put(Constants.SERVICE_PID, pid);
        props.put("instance.name", getString("name", props));
        Enumeration<String> keys = props.keys();
        while (keys.hasMoreElements()) {
            final String key = keys.nextElement();
            LOGGER.info("{}={}", key, props.get(key));
        }  
        dataSourceParams.serviceRegistration = context.registerService(DataSource.class.getCanonicalName(), dataSource, props);
        return pid;
    }

    public void deleteDataSource(String pid) {
        final DataSourceParams dsp = pid2dataSourceParams.get(pid);
        if (dsp != null) {
            LOGGER.info("DataSourceFactory:deleteDataSource:{}", pid);
            context.ungetService(dsp.serviceRegistration.getReference());
            pid2dataSourceParams.remove(pid);
        }
    }

    private final List<DriverAndServiceReference> drivers = new LinkedList<DriverAndServiceReference>();

    private final BundleContext context;

    public DataSourceFactory(BundleContext context) {
        this.context = context;
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
    public void addDriver(DriverFactory driver, ServiceReference<DriverFactory> sr) {
        final String driverSignature = String.format("%s-%s-%s", 
            driver.getClass().getCanonicalName(), 
            sr.getProperty("name"), sr.getProperty("fullName"));
        LOGGER.info("DataSourceFactory:addDriver:{}", driver.getClass().getCanonicalName());
        drivers.add(new DriverAndServiceReference(driver, sr));
        // Replay Connection 
        for(Map.Entry<String, DataSourceParams> entry: pid2dataSourceParams.entrySet()) {
            if (isDriver(entry.getValue().props.get("driver"), sr)) {
                if (entry.getValue().serviceRegistration != null) {
                    LOGGER.error("addDriver has an registered DataSources:{}",driverSignature);
                    continue;
                }
                createDataSource(entry.getValue().props);
            }
        }
    }

    private boolean isDriver(String name, ServiceReference sr) {
        final String srName = (String)sr.getProperty("name");
        if (srName != null && srName.equals(name)) {
            return true;
        }
        final String srFullName = (String)sr.getProperty("fullName");
        if (srFullName != null && srFullName.equals(name)) {
            return true;
        }
        return false;
    }
   
    private boolean isDriver(Dictionary<String, String> prop, ServiceReference sr2) {
        return isDriver(prop.get("name"), sr2) ||
               isDriver(prop.get("fullName"), sr2);
    }

    private boolean isDriver(ServiceReference sr1, ServiceReference sr2) {
        return isDriver(sr1.getProperty("name").toString(), sr2) ||
               isDriver(sr1.getProperty("fullName").toString(), sr2);
    }

    private DriverAndServiceReference findByName(String name) {
        for (DriverAndServiceReference driver : drivers) {
            LOGGER.info("findByName:{}=={}||{}", name, 
                driver.serviceReference.getProperty("name"),
                driver.serviceReference.getProperty("fullName"));
            if (isDriver(name, driver.serviceReference)) {
                return driver;
            }
        }
        return null;
    }

    @Unbind(optional = true, aggregate = true)
    public void delDriver(DriverFactory driver, ServiceReference<DriverFactory> sr) {
        try {
            final String name = sr.getProperty("name").toString();
            final String fullName = sr.getProperty("fullName").toString();
            LOGGER.info("DataSourceFactory:delDriver:{}", driver.getClass().getCanonicalName());
            drivers.remove(findByName(name));
            drivers.remove(findByName(fullName));
            final List<DataSourceParams> toRenew = new LinkedList<DataSourceParams>();
            for(Map.Entry<String, DataSourceParams> entry: pid2dataSourceParams.entrySet()) {
                if (isDriver(sr, entry.getValue().serviceRegistration.getReference())) {
                    toRenew.add(entry.getValue());
                }
            }
            for(DataSourceParams dsp : toRenew) {
                deleteDataSource(dsp.props.get(Constants.SERVICE_PID));
                dsp.serviceRegistration = null;
                pid2dataSourceParams.put(dsp.props.get(Constants.SERVICE_PID), dsp);
            }
        } catch (Exception e) {
            LOGGER.error("Error while deleting JDBC driver: ", e.getMessage());
        }
    }
    
    protected static String getString(final String key, final Dictionary<?, ?> properties) {
        final Object value = properties.get(key);
        return (value instanceof String) ? (String) value : null;
    }


    public static DataSource create(final Driver driver, Dictionary<String, String> prop) {
        final Properties driverProp = new Properties();
        final Enumeration<String> keys = prop.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            driverProp.setProperty(key, prop.get(key));
        }
        final String uri = getString("url", prop);
        int jdbcIdx = uri.indexOf(':');
        if (jdbcIdx <= 0) {
            LOGGER.error("url not parsable:{}", uri);
            return null;
        }
        int uriIdx = uri.indexOf(':', jdbcIdx+1);
        if (uriIdx <= 0) {
            LOGGER.error("url not parsable:{}", uri);
            return null;
        }    
        LOGGER.info("DataSource:createDriver:uri={}", uri);
        final ConnectionFactory connectionFactory = new ConnectionFactory() {
      
            @Override
            public Connection createConnection() throws SQLException {
                final Connection connection = driver.connect(uri, driverProp);
                LOGGER.info("ConnectionFactory:createConnection:{}:{}:{}", 
                    driver.getClass().getName(),
                    uri, connection);
                return connection;
            }
         
        };

        final GenericObjectPool connectionPool = new GenericObjectPool();
        connectionPool.setMaxActive(30);
        final KeyedObjectPoolFactory stmtPoolFactory = new GenericKeyedObjectPoolFactory(null);

        final PoolableConnectionFactory factory = new PoolableConnectionFactory(
            connectionFactory,
            connectionPool,
            stmtPoolFactory,
            getString("driver.sql.validation.query", prop),
            true,
            false);
        return new PoolingDataSource(factory.getPool());

    }

}
