package de.nextaudience.db.datasource.base;

import java.sql.Driver;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings({ "rawtypes", "unchecked" })


//@Component(immediate=true,name="de.nextaudience.db.datasource.postgresql",factory="datasource.postgresql")
////public class DataSourceFactory implements ManagedServiceFactory {
  
public class DataSourceFactory implements ManagedServiceFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceFactory.class);
    
    private final Map<String, DataSourceAndServiceRegistration> services = new HashMap<String, DataSourceAndServiceRegistration>();
    private final BundleContext ctx;
    private final Driver driver;

    public DataSourceFactory(BundleContext ctx, Driver driver) {
        this.ctx = ctx;
        this.driver = driver;
    }
    
    public Driver getDriver() {
        return driver;
    }
    
    public String getName() {
        return getDriver().getClass().getCanonicalName();
    }

    public void updated(String pid, Dictionary properties)
            throws ConfigurationException {
        LOGGER.debug("retrieved update for pid " + pid);
        DataSourceAndServiceRegistration reg = services.get(pid);
        if (reg != null) { // update
            LOGGER.info("UPDATED:unregister:"+reg.applicationName);
            reg.unregister();
            reg = null;
        }
        if (reg == null) {
            properties.put("pid", pid);            
            final DataSourceAndServiceRegistration dasr = new DataSourceAndServiceRegistration(getDriver(), properties, ctx);        
            services.put(pid, dasr);
            LOGGER.info("UPDATED:new:"+dasr.applicationName);
        }
    }

    @Override
    public void deleted(String pid) {        
        for (DataSourceAndServiceRegistration reg : services.values()) {
            LOGGER.info("deregister " + reg);
            reg.unregister();
        }
    }

}