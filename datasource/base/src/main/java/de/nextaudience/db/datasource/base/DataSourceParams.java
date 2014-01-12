/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.nextaudience.db.datasource.base;

import java.util.Dictionary;
import org.osgi.framework.ServiceRegistration;

/**
 *
 * @author menabe
 */
public class DataSourceParams {
    public ServiceRegistration serviceRegistration;
    public final Dictionary<String, String> props;
    
    /**
     *
     */
    public DataSourceParams(Dictionary<String, String> props) {
        this.props = props;
    }
}
