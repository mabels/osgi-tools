package de.nextaudience.db.datasource.postgresql;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private ServiceRegistration<ManagedServiceFactory> myReg;
   
    @Override
    public void start(BundleContext context) throws Exception {
        LOGGER.info("starting factory... de.nextaudience.db.datasource.postgresql");
        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(Constants.SERVICE_PID, "de.nextaudience.db.datasource.postgresql");
        
        myReg = context.registerService(ManagedServiceFactory.class, 
                    new DataSourceFactory(context), 
                    properties);
        LOGGER.debug("registered as ManagedServiceFactory");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        LOGGER.info("stop factory... de.nextaudience.db.datasource.postgresql");
        if (myReg != null) {
            myReg.unregister();
        }
    }
}
