package de.nextaudience.db.datasource.postgresql;

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

    public DataSourceFactory(BundleContext ctx) {
        this.ctx = ctx;
    }
    
    @Override
    public String getName() {
        System.out.println("returning facotry name");
        return "de.nextaudience.db.datasource.postgresql";
    }
    

    @Override
    public void updated(String pid, Dictionary properties)
            throws ConfigurationException {
        LOGGER.debug("retrieved update for pid " + pid);
        DataSourceAndServiceRegistration reg = services.get(pid);
        if (reg != null) { // update
            LOGGER.info("GOTUPDATE:"+reg.applicationName);
            reg.unregister();
            reg = null;
        }
        if (reg == null) {
            properties.put("pid", pid);
            
            final DataSourceAndServiceRegistration dasr = new DataSourceAndServiceRegistration(properties, ctx);
            
            services.put(pid, dasr);
            LOGGER.info("NEW:"+dasr.applicationName);
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