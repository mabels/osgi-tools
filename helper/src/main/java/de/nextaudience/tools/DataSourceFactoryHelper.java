package de.nextaudience.tools;

import javax.sql.DataSource;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import de.nextaudience.db.datasource.dynamic.DynamicDataSourceFactory;
import java.util.Dictionary;


public class DataSourceFactoryHelper {

    private DynamicDataSourceFactory dataSourceFactory;
    private String url;
    private String pid;
    private final OSGiHelper osgiHelper;

    public DataSourceFactoryHelper(final BundleContext bundleContext) {
        this.osgiHelper = new OSGiHelper(bundleContext);
    }

    public void create(final DynamicDataSourceFactory dataSourceFactory, final String pid, final String name, final String url, final String user,
            final String password) throws Exception {
        if (this.url != null) {
            throw new RuntimeException("Datasource must be disposed before reusing a DataSourceFactoryHelper.");
        }
        @SuppressWarnings("rawtypes")
        ServiceReference sr = this.osgiHelper.getServiceReferenceByPID(DataSource.class, pid);
        if (sr == null) {
            this.dataSourceFactory = dataSourceFactory;
            this.pid = pid;
            this.url = url;
            dataSourceFactory.createDataSource(pid, name, url, user, password);
        } else {
            this.dataSourceFactory = null;
            this.pid = null;
            this.url = null;
        }
    }

    public void dispose() {
        if (this.url != null) {
            this.dataSourceFactory.deleteDataSource(this.pid, this.url);
        }
    }

}
