package com.adviser.osgi.helper;

import java.util.Dictionary;

import javax.sql.DataSource;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import com.adviser.osgi.db.datasource.DataSourceFactory;


public class DataSourceFactoryHelper {

    private final OSGiHelper osgiHelper;

    public DataSourceFactoryHelper(final BundleContext bundleContext) {
        this.osgiHelper = new OSGiHelper(bundleContext);
    }

    public String create(final DataSourceFactory dataSourceFactory, final Dictionary<String, String> props) throws Exception {
        String pid = null;
        final String newpid = props.get(Constants.SERVICE_PID);
        @SuppressWarnings("rawtypes")
        ServiceReference sr = this.osgiHelper.getServiceReferenceByPID(DataSource.class, newpid);
        if (sr == null) {
            // Service not registered => create a new datasource
            pid = dataSourceFactory.createDataSource(props);
            if (pid == null) {
                // if no pid is returned, the datasource creation failed
                throw new RuntimeException("Datasource for '" + props.toString() + "' could not be created.");
            }
        } else {
            // do nothing. The requested datasource PID is already registered in OSGI and somebody else has to take care of it
            pid = null;
        }
        return pid;
    }

    public void dispose(final DataSourceFactory dataSourceFactory, final String pid) {
        if (dataSourceFactory != null && pid != null) {
            dataSourceFactory.deleteDataSource(pid);
        }
        osgiHelper.dispose();
    }

}
