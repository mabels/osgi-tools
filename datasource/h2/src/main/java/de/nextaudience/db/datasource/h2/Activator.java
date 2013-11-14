package de.nextaudience.db.datasource.h2;

import org.osgi.framework.BundleContext;

import de.nextaudience.db.datasource.base.DataSourceFactory;

public class Activator extends de.nextaudience.db.datasource.base.Activator {

     @Override
    protected DataSourceFactory makeDataSourceFactory(BundleContext context) {
        return new DataSourceFactory(context, new org.h2.Driver());
    }
}
