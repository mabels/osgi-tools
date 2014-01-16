package de.nextaudience.db.datasource.base;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Updated;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience class to gather all db connections into one file that can get deployed as feature.
 *
 * Should not be used in parallel to the other activators as there is currently no detection of duplicate db configurations.
 *
 * This class reuses the h2 and postgres activatirs with its own bundle context.
 */
@Component
public class DataSourcesCfgFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourcesCfgFile.class);

    private static final String PROP_DATASOURCES = "datasources";

    private final BundleContext bundleContext;

    @Requires
    private de.nextaudience.db.datasource.DataSourceFactory dataSourceFactory;

    private boolean waitForStart = true;
    private Dictionary<String, String> properties = null;
    private final List<String> dataSources = new LinkedList<String>();


    public DataSourcesCfgFile(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Updated
    public void updated(Dictionary<String, String> properties) {
        LOGGER.info("DataSourcesCfgFile:updated");
        if (properties == null || properties.isEmpty()) {
            close();
            return;
        }
        if (waitForStart) {
            this.properties = properties;
        } else {
            close();
            for (Map.Entry<String, Dictionary<String, String>> entry : splittedProperties(properties).entrySet()) {
                final String name = entry.getKey();
                final Dictionary<String, String> dict = entry.getValue();
                LOGGER.info("DataSourcesCfgFile:updated:{}", dict.get("name"));
                Enumeration<String> keys = dict.keys();
                while (keys.hasMoreElements()) {
                    final String key = keys.nextElement();
                    LOGGER.info("DataSourcesCfgFile:updated:{}=>{}={}", name, key, dict.get(key));
                }
                dict.put(Constants.SERVICE_PID, String.format("DS-%s",
                    DataSourceFactory.getString("name", dict)));
                String pid = dataSourceFactory.createDataSource(dict);
                if (pid != null) {
                    dataSources.add(pid);
                }
            }
        }
    }

    @Validate
    public void validate() {
        LOGGER.info("DataSourcesCfgFile:start");
        waitForStart = false;
        if (properties != null) {
            updated(properties);
            properties = null;
        }
    }

    @Invalidate
    public void invalidate() {
        LOGGER.info("DataSourcesCfgFile:stop");
        waitForStart = true;
        close();
    }

    private void close() {
        for (String pid : dataSources) {
            LOGGER.info("DataSourcesCfgFile:close:{}", pid);
            dataSourceFactory.deleteDataSource(pid);
        }
        dataSources.clear();
    }

    private static Map<String, Dictionary<String, String>> splittedProperties(Dictionary<String, String> properties) {
        //LOGGER.info("splittedProperties:{}", properties);
        String datasources = properties.get(PROP_DATASOURCES);
        if (datasources == null) {
            throw new IllegalArgumentException("Missing mandatory property '" + PROP_DATASOURCES + "'");
        }

        final Map<String, Dictionary<String, String>> dicts = new HashMap<>();
        List<String> validKeys = Arrays.asList(datasources.split(","));
        Enumeration<String> keys = properties.keys();
        while (keys.hasMoreElements()) {
            final String key = keys.nextElement();
            final SplittedKey split = SplittedKey.create(key);
            if (split == null || !validKeys.contains(split.first)) {
                continue;
            }
            Dictionary<String, String> d = dicts.get(split.first);
            if (d == null) {
                d = new Hashtable<String, String>();
                d.put("name", split.first);
                dicts.put(split.first, d);
            }
            d.put(split.tail, properties.get(key));
        }
        return dicts;
    }

}
