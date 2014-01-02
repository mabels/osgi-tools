package de.nextaudience.db.datasource.aggregation;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Updated;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.nextaudience.db.datasource.base.DataSourceFactory;

/**
 * Convenience class to gather all db connections into one file that can get deployed as feature.
 *
 * Should not be used in parallel to the other activators as there is currently no
 * detection of duplicate db configurations.
 *
 * This class reuses the h2 and postgres activatirs with its own bundle context.
 */
@Component(managedservice = "de.nextaudience.db.datasource.aggregation")
@Instantiate
public class Activator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private static final String SERVICE_PID = "de.nextaudience.db.datasource.aggregation";

    private final BundleContext bundleContext;

    private final Map<String, DataSourceFactory> factories = new HashMap<>();

    public Activator(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Updated
    public synchronized void updated(Dictionary<String, String> properties) throws ConfigurationException {
        LOGGER.info("Updated {}", properties);

        String namesAsString = properties.get("names");
        if (namesAsString == null || namesAsString.trim().isEmpty()) {
            LOGGER.warn("Property [names] not found, removing all active connections");
            invalidate();
        } else {
            String[] connectionNames = namesAsString.replaceAll("\\s", "").split(",");
            Set<String> activeConnections = new HashSet<>(Arrays.asList(connectionNames));
            closeOldConnections(activeConnections);

            for (String name : connectionNames) {
                createOrUpdateConnection(properties, name);
            }
        }
    }

    private void closeOldConnections(Set<String> activeConnections) {
        Set<String> oldConnections = new HashSet<>(this.factories.keySet());
        oldConnections.removeAll(activeConnections);

        for (String name : oldConnections) {
            closeConnection(name);
        }
    }

    private void createOrUpdateConnection(Dictionary<String, String> properties, String name) throws ConfigurationException {
        DataSourceFactory dataSourceFactory = this.factories.get(name);
        if (dataSourceFactory == null) {
            String driver = properties.get(name + ".driver");
            dataSourceFactory = getFactoryForDriver(driver);
            this.factories.put(name, dataSourceFactory);
            LOGGER.info("\tcreate connection {}", name);
        } else {
            LOGGER.info("\tupdate connection {}", name);
        }

        Dictionary<String, String> dictionary = new ConnectionDictionary(name, properties);
        dataSourceFactory.updated(createConnectionPid(name), dictionary);
    }

    private DataSourceFactory getFactoryForDriver(String driver) {
        DataSourceFactory dataSourceFactory;
        // this is ugly
        if ("h2".equals(driver)) {
            dataSourceFactory = de.nextaudience.db.datasource.h2.Activator.getInstance().makeDataSourceFactory(this.bundleContext);
        } else {
            dataSourceFactory = de.nextaudience.db.datasource.postgresql.Activator.getInstance().makeDataSourceFactory(this.bundleContext);
        }
        return dataSourceFactory;
    }

    private void closeConnection(String connectionName) {
        LOGGER.info("\tshutting down {}", connectionName);

        DataSourceFactory factory = this.factories.get(connectionName);

        if(factory != null) {
            factory.deleted(createConnectionPid(connectionName));
            this.factories.remove(connectionName);
        }

        LOGGER.info("\tshutting down {} - done", connectionName);
    }

    private String createConnectionPid(String name) {
        return SERVICE_PID + "_" + name;
    }

    @Invalidate
    public synchronized void invalidate() {
        Set<String> connections = new HashSet<>(this.factories.keySet());
        LOGGER.info("Invalidate aggregated databases ({})", connections);
        for (String connectionName : connections) {
            closeConnection(connectionName);
        }

        this.factories.clear();
    }


    private static class ConnectionDictionary extends Hashtable<String, String> {

        private static final long serialVersionUID = 1L;
        private final String name;
        private final Dictionary<String, String> properties;


        public ConnectionDictionary(String header, Dictionary<String, String> properties) {
            this.name = header;
            this.properties = properties;
        }

        @Override
        public synchronized String get(Object key) {
            if ("name".equals(key)) {
                return this.name;
            }
            return this.properties.get(this.name + "." + key);
        }

    }
}
