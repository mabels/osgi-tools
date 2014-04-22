package com.adviser.osgi.hibernate;

import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.osgi.framework.Bundle;

public interface HibernateSessionFactoryCreator {

    SessionFactory create(final Bundle requestingBundle, final String hibernateCfg, final ConnectionProvider connectionProvider,
            final DialectFactory dialectFactory, final Properties configurationProperties);

    void unregisterBundle(final Bundle requestingBundle);
}
