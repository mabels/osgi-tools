package de.nextaudience.db.datasource.postgresql;

import org.osgi.framework.BundleContext;

import de.nextaudience.db.datasource.base.DataSourceFactory;

public class Activator extends de.nextaudience.db.datasource.base.Activator {

    @Override
    protected DataSourceFactory makeDataSourceFactory(BundleContext context) {
        return new DataSourceFactory(context, new org.postgresql.Driver(), "org.hibernate.dialect.PostgreSQLDialect");
    }
}
