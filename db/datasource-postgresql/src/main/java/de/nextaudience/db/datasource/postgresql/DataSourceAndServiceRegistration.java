package de.nextaudience.db.datasource.postgresql;

import java.sql.SQLException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DataSourceAndServiceRegistration {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceAndServiceRegistration.class);
    
    final public BasicDataSource dataSource = new OsgiBasicDataSource(new org.postgresql.Driver());
    final public String applicationName;
    final public ServiceRegistration<DataSource> serviceRegistration;
    
    private static String getString(String key, Dictionary<?, ?> properties) {
        Object value = properties.get(key);
        return (value == null || !(value instanceof String)) ? "" : (String)value;   
    }
    
    private static String getApplicationName(BundleContext ctx, Dictionary<?, ?> properties) {
        return "DS-"+ctx.getBundle().getBundleId()+"-"+getString("pid", properties)+"-"+getString("name", properties);
    }


    public DataSourceAndServiceRegistration(Dictionary<?, ?> connectionProp, BundleContext ctx) {
        LOGGER.debug("+DataSourceAndServiceRegistration:"+getString("url", connectionProp));
        applicationName = getApplicationName(ctx, connectionProp);    
        dataSource.setUsername(getString("user", connectionProp));
        dataSource.setPassword(getString("password", connectionProp));
        dataSource.setUrl(getString("url", connectionProp));
        
        final Dictionary<String, String> regProperties = new Hashtable<String, String>();
        regProperties.put("osgi.jndi.service.name" , DataSourceAndServiceRegistration.getString("name", connectionProp)); 

        serviceRegistration = ctx.registerService(DataSource.class, dataSource, regProperties);
    }
    public void unregister() {
        LOGGER.info("-DataSourceAndServiceRegistration:"+dataSource.getUrl());
        try {
            dataSource.close();
        } catch (SQLException e) {
            LOGGER.error("dataSource.close failed", e);
        }
        serviceRegistration.unregister();
    }
}
