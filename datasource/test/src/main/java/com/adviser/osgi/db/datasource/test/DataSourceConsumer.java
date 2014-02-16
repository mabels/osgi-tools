package com.adviser.osgi.db.datasource.test;

import javax.sql.DataSource;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@SuppressWarnings({ "rawtypes", "unchecked" })


 
@Component(immediate=true)
@Instantiate
public class DataSourceConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceConsumer.class);

    @Requires(filter="(osgi.jndi.service.name=adm)")
    private DataSource adm;
    @Requires(filter="(osgi.jndi.service.name=adc)")
    private DataSource adc;
    
    @Validate
    public void start() {
        LOGGER.info("START DataSourceConsumer:"+adm+":"+adc);
    }
    
    @Invalidate
    public void stop() {
        LOGGER.info("STOP DataSourceConsumer:"+adm+":"+adc);        
    }

}
