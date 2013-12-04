package de.nextaudience.db.datasource.base;

import java.sql.Driver;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
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
    private final String dialect;

    public DataSourceFactory(BundleContext ctx, Driver driver, String dialect) {
        this.ctx = ctx;
        this.driver = driver;
        this.dialect = dialect;
    }


    public String getDialect() {
        return dialect;
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
            properties.put(Constants.SERVICE_PID, pid);
            final DataSourceAndServiceRegistration dasr = new DataSourceAndServiceRegistration(getDriver(), getDialect(), properties, ctx);
            services.put(pid, dasr);
            LOGGER.info("UPDATED:new:"+dasr.applicationName);
        }
    }

    @Override
    public void deleted(String pid) {
        DataSourceAndServiceRegistration reg = services.get(pid);
        if (reg != null) { 
            LOGGER.info("deleting:"+reg.applicationName);
            reg.unregister();
            services.remove(pid);
        }
    }

}