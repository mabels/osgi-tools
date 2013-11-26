package de.nextaudience.db.datasource.test;

import javax.sql.DataSource;

import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@SuppressWarnings({ "rawtypes", "unchecked" })


//@Component(immediate=true,name="de.nextaudience.db.datasource.postgresql",factory="datasource.postgresql")
  
@Component
public class DataSourceConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceConsumer.class);

    @Requires(filter="(osgi.jndi.service.name=adm)")
    DataSource adm;
    @Requires(filter="(osgi.jndi.service.name=adc)")
    DataSource adc;
    
    @Validate
    public void start() {
        LOGGER.info("START DataSourceConsumer:"+adm+":"+adc);
    }
    
    @Invalidate
    public void stop() {
        LOGGER.info("START DataSourceConsumer:"+adm+":"+adc);        
    }

}
