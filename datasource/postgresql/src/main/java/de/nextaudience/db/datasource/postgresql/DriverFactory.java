package de.nextaudience.db.datasource.postgresql;

import org.osgi.framework.BundleContext;

import java.sql.Driver;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
@Instantiate
@Provides
public class DriverFactory implements de.nextaudience.db.datasource.DriverFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DriverFactory.class);

    private final BundleContext context;

    @ServiceProperty(name = "hibernate.dialect", value = "org.hibernate.dialect.PostgreSQLDialect")
    private String dialect;
    @ServiceProperty(name = "sql.validation.query", value = "select 1")
    private String query;
    @ServiceProperty(value = "org.postgresql.Driver")
    private String fullName;
    @ServiceProperty(value = "postgresql")
    private String name;

    private Driver driver;

    public DriverFactory(BundleContext context) {
        this.context = context;
    }

    @Validate
    public void start() {
        LOGGER.info("DriverFactory:postgresql:start");
        driver = new org.postgresql.Driver();
    }

    @Invalidate
    public void stop() {
        LOGGER.info("DriverFactory:postgresql:stop");
        driver = null;
    }

    public Driver get() {
        return driver;
    }

}
