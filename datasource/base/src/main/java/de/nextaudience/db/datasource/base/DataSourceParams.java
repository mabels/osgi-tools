/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.nextaudience.db.datasource.base;

import java.util.Dictionary;

import javax.sql.DataSource;

import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.osgi.framework.ServiceRegistration;

import de.nextaudience.tools.IPOJOInstanceHelper;

/**
 *
 * @author menabe
 */
public class DataSourceParams {
    public ServiceRegistration serviceRegistration;
    public final Dictionary<String, String> props;

    public PoolableConnectionFactory poolableConnectionFactory;
    public PoolingDataSource poolingDataSource;
    public IPOJOInstanceHelper.InstanceHolder<DataSource> instanceHolder;
    
    /**
     *
     */
    public DataSourceParams(Dictionary<String, String> props) {
        this.props = props;
    }
}
