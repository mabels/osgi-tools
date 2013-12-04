package de.nextaudience.db.datasource.dynamic;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Unbind;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
@Instantiate
@Provides
public class DynamicDataSourceFactoryImpl implements DynamicDataSourceFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DynamicDataSourceFactoryImpl.class);
    private final Map<String, ManagedServiceFactory> factories = new HashMap<String, ManagedServiceFactory>();


    @Bind(aggregate = true)
    public void bind(final ManagedServiceFactory sf, final Dictionary<?, ?> properties) {
        if (getString(Constants.SERVICE_PID, properties) != null && getString("database.type", properties) != null) {
            LOG.info("Binding datasource factory " + sf.getName());
            this.factories.put(getString("database.type", properties), sf);
        }
    }

    @Unbind
    public void unbind(final ManagedServiceFactory sf, final Dictionary<?, ?> properties) {
        if (getString(Constants.SERVICE_PID, properties) != null && getString("database.type", properties) != null) {
            LOG.info("Unbinding datasource factory " + sf.getName());
            this.factories.remove(getString("database.type", properties));
        }

    }

    public void createDataSource(final String pid, final String name, final String url, final String user, final String password)
            throws Exception {
        LOG.info("Creating dynamic datasource " + pid);
        final String dbType = getDatabaseType(url);
        if (dbType != null) {
            final Dictionary<String, String> properties = new Hashtable<String, String>();
            properties.put("pid", pid);
            properties.put("name", name);
            properties.put("user", user);
            properties.put("password", password);
            properties.put("url", url);
            
            properties.put("instance.name", name);
            this.factories.get(dbType).updated(pid, properties);
        } else {
            // TODO: throw exception?
        }
    }

    public void deleteDataSource(String pid, String url) {
        LOG.info("Deleting dynamic datasource " + pid);
        final String dbType = getDatabaseType(url);
        if (dbType != null) {
            this.factories.get(dbType).deleted(pid);
        }
    }

    private static String getString(final String key, final Dictionary<?, ?> properties) {
        final Object value = properties.get(key);
        return (value instanceof String) ? (String) value : null;
    }

    private String getDatabaseType(final String url) {
        if (url.startsWith("jdbc:postgresql:")) {
            return "org.postgresql.Driver";
        } else if (url.startsWith("jdbc:h2:")) {
            return "org.h2.Driver";
        }
        return null;
    }

}
