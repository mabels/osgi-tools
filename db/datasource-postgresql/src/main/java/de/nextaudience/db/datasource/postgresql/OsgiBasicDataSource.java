package de.nextaudience.db.datasource.postgresql;

import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class OsgiBasicDataSource extends BasicDataSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(OsgiBasicDataSource.class);
    
    
    private Driver driver;
    OsgiBasicDataSource(Driver driver) {
      LOGGER.debug("+OsgiBasicDataSource:"+driver.getClass().getCanonicalName()+":"+driver.toString());
      this.driver = driver;
    }
    @Override
    protected ConnectionFactory createConnectionFactory() throws SQLException {
      LOGGER.debug(">OsgiBasicDataSource:createConnectionFactory"); 
      Properties connectionProperties = new Properties();
      if (this.getUsername() != null) {
        connectionProperties.put("user", this.getUsername());
      }
      if (this.getPassword() != null) {
        connectionProperties.put("password", this.getPassword());
      }
      DriverConnectionFactory connectionFactory =
        new DriverConnectionFactory(driver, url, connectionProperties);
      LOGGER.debug("<OsgiBasicDataSource:createConnectionFactory="+connectionFactory);   
      return connectionFactory;
    }
}
