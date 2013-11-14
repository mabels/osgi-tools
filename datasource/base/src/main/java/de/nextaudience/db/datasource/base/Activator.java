package de.nextaudience.db.datasource.base;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Activator implements BundleActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);
   
    protected abstract DataSourceFactory makeDataSourceFactory(BundleContext context);

    private ServiceRegistration myReg;

    @Override
    public void start(BundleContext context) throws Exception {
        final DataSourceFactory dataSourceFactory = makeDataSourceFactory(context);
        LOGGER.info("starting factory..." + dataSourceFactory.getName());
        final Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(Constants.SERVICE_PID, dataSourceFactory.getName());
        myReg = context.registerService(ManagedServiceFactory.class.getCanonicalName(), dataSourceFactory, properties);
        LOGGER.debug("registered as ManagedServiceFactory" + dataSourceFactory.getName());
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (myReg != null) {
            myReg.unregister();
        }
    }
}
