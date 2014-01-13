package de.nextaudience.tools;

import javax.sql.DataSource;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import de.nextaudience.db.datasource.DataSourceFactory;


public class DataSourceFactoryHelper {

    private DataSourceFactory dataSourceFactory;
    private String url;
    private String pid;
    private final OSGiHelper osgiHelper;

    public DataSourceFactoryHelper(final BundleContext bundleContext) {
        this.osgiHelper = new OSGiHelper(bundleContext);
    }

    public void create(final DataSourceFactory dataSourceFactory, final String pid, final String name, final String url, final String user,
            final String password) throws Exception {
        if (this.url != null) {
            throw new RuntimeException("Datasource must be disposed before reusing a DataSourceFactoryHelper.");
        }
        @SuppressWarnings("rawtypes")
        ServiceReference sr = this.osgiHelper.getServiceReferenceByPID(DataSource.class, pid);
        if (sr == null) {
            this.dataSourceFactory = dataSourceFactory;
            this.url = url;
            this.pid = dataSourceFactory.createDataSource(name, url, user, password);
        } else {
            this.dataSourceFactory = null;
            this.pid = null;
            this.url = null;
        }
    }

    public void dispose() {
        if (this.url != null) {
            this.dataSourceFactory.deleteDataSource(this.pid);
        }
    }

}
