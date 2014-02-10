package de.nextaudience.db.hibernate;

import java.util.Map;
import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.osgi.framework.Bundle;

public interface HibernateSessionFactoryCreator {

    SessionFactory create(final Bundle requestingBundle, final String hibernateCfg, final ConnectionProvider connectionProvider,
            final DialectFactory dialectFactory, final Map<String, NamedSQLQueryDefinition> namedSQLQueries, final Properties configurationProperties);

    void unregisterBundle(final Bundle requestingBundle);
}