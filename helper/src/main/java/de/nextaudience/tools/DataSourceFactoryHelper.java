package de.nextaudience.tools;

import java.util.Dictionary;

import javax.sql.DataSource;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import de.nextaudience.db.datasource.DataSourceFactory;


public class DataSourceFactoryHelper {

    private DataSourceFactory dataSourceFactory;
    private String pid;
    private final OSGiHelper osgiHelper;

    public DataSourceFactoryHelper(final BundleContext bundleContext) {
        this.osgiHelper = new OSGiHelper(bundleContext);
    }

    public void create(final DataSourceFactory dataSourceFactory, final Dictionary<String, String> props) throws Exception {
        if (this.pid != null) {
            throw new RuntimeException("Datasource must be disposed before reusing a DataSourceFactoryHelper.");
        }
        
        final String newpid = props.get(Constants.SERVICE_PID);
        @SuppressWarnings("rawtypes")
        ServiceReference sr = this.osgiHelper.getServiceReferenceByPID(DataSource.class, newpid);
        if (sr == null) {
            // Service not registered => create a new datasource
            this.dataSourceFactory = dataSourceFactory;
            this.pid = dataSourceFactory.createDataSource(props);
            if (this.pid == null) {
                // if no pid is returned, the datasource creation failed
                throw new RuntimeException("Datasource for '" + props.toString() + "' could not be created.");
            }
        } else {
            // do nothing. The requested datasource PID is already registered in OSGI and somebody else has to take care of it
            this.dataSourceFactory = null;
            this.pid = null;
        }
    }

    public void dispose() {
        if (this.pid != null) {
            this.dataSourceFactory.deleteDataSource(this.pid);
        }
    }

}
