package com.adviser.osgi.hibernate;

import java.net.URL;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.internal.util.ClassLoaderHelper;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.metamodel.spi.TypeContributor;
import org.hibernate.osgi.OsgiJtaPlatform;
import org.hibernate.osgi.OsgiServiceUtil;
import org.hibernate.osgi.OsgiSessionFactoryService;
import org.hibernate.service.ServiceRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
@Instantiate
@Provides(specifications = { HibernateSessionFactoryCreator.class })
public class HibernateSessionFactoryService extends OsgiSessionFactoryService implements HibernateSessionFactoryCreator {

    private static final Logger LOG = LoggerFactory.getLogger(HibernateSessionFactoryService.class);

    private final OsgiClassLoader osgiClassLoader;
    private final OsgiJtaPlatform osgiJtaPlatform;
    private OsgiServiceUtil osgiServiceUtil;
    private BundleContext bundleContext;


    public HibernateSessionFactoryService(final BundleContext context) throws Exception {
        super(null, null, null);
        this.bundleContext = context;
        // build a ClassLoader that uses all the necessary OSGi bundles, and place it into
        // a well-known location so internals can access it
        this.osgiClassLoader = new OsgiClassLoader();
        this.osgiClassLoader.addBundle(FrameworkUtil.getBundle(Session.class));
        this.osgiClassLoader.addBundle(FrameworkUtil.getBundle(HibernatePersistenceProvider.class));
        this.osgiServiceUtil = new OsgiServiceUtil(context);
        this.osgiJtaPlatform = new OsgiJtaPlatform(this.osgiServiceUtil);
    }

    @Validate
    public void start() {
        LOG.info(getClass().getName() + "::Start");
        ClassLoaderHelper.overridenClassLoader = this.osgiClassLoader;
    }

    @Invalidate
    public void stop() {
        ClassLoaderHelper.overridenClassLoader = null;
        LOG.info(getClass().getName() + "::Stop");
    }

    /** {@inheritDoc} */
    @Override
    public SessionFactory create(final Bundle requestingBundle, final String hibernateCfg,
            final ConnectionProvider connectionProvider, final DialectFactory dialectFactory,
            final Properties configurationProperties) {
        this.osgiClassLoader.addBundle(requestingBundle);

        final Configuration configuration = new Configuration();
        configuration.getProperties().put(AvailableSettings.JTA_PLATFORM, this.osgiJtaPlatform);
        // allow to additional custom properties for Session creation
        if (configurationProperties != null) {
            for (Entry<Object, Object> e : configurationProperties.entrySet()) {
                configuration.getProperties().put(e.getKey(), e.getValue());
            }
        }

        configuration.configure(hibernateCfg);

        final BootstrapServiceRegistryBuilder builder = new BootstrapServiceRegistryBuilder();
        registerClasses(builder);

        final Integrator[] integrators = this.osgiServiceUtil.getServiceImpls(Integrator.class);
        for (final Integrator integrator : integrators) {
            builder.with(integrator);
        }

        final StrategyRegistrationProvider[] strategyRegistrationProviders = this.osgiServiceUtil
                .getServiceImpls(StrategyRegistrationProvider.class);
        for (final StrategyRegistrationProvider strategyRegistrationProvider : strategyRegistrationProviders) {
            builder.withStrategySelectors(strategyRegistrationProvider);
        }

        final TypeContributor[] typeContributors = this.osgiServiceUtil.getServiceImpls(TypeContributor.class);
        for (final TypeContributor typeContributor : typeContributors) {
            configuration.registerTypeContributor(typeContributor);
        }

        final StandardServiceRegistryBuilder srb = new StandardServiceRegistryBuilder(builder.build());
        srb.addService(ConnectionProvider.class, connectionProvider);
        srb.addService(DialectFactory.class, dialectFactory);
        final ServiceRegistry serviceRegistry = srb.applySettings(configuration.getProperties()).build();

        Thread t = Thread.currentThread();
        ClassLoader old = t.getContextClassLoader();
        try {
            t.setContextClassLoader(this.osgiClassLoader);
            return configuration.buildSessionFactory(serviceRegistry);
        } finally {
            t.setContextClassLoader(old);
        }
    }

    private void registerClasses(final BootstrapServiceRegistryBuilder builder) {
        builder.with(this.osgiClassLoader);
        // Are the following 2 really needed?
        builder.with(org.hibernate.cache.ehcache.EhCacheRegionFactory.class.getClassLoader());
        builder.with(net.sf.ehcache.statistics.sampled.SampledCacheStatistics.class.getClassLoader());
        builder.with(javassist.util.proxy.ProxyObject.class.getClassLoader());
        builder.with(org.hibernate.proxy.HibernateProxy.class.getClassLoader());
        addClassesDynamically(builder);
    }

    private void addClassesDynamically(final BootstrapServiceRegistryBuilder builder) {
        for (Bundle bundle : this.bundleContext.getBundles()) {
            String classList = bundle.getHeaders().get("HibernateSessionFactoryService");
            if (classList != null && !classList.trim().isEmpty()) {
                LOG.info("Adding classes of bundle [{}] / {}", bundle.getSymbolicName(), bundle.getBundleId());
                String[] classes = classList.replaceAll("\\s", "").split(",");
                for (String className : classes) {
                    try {
                        LOG.info("Adding class [{}]", className);
                        Class c = bundle.loadClass(className);
                        builder.with(c.getClassLoader());
                    } catch (ClassNotFoundException e) {
                        LOG.warn("Could not find or load class {}", className);
                        String path = "/" + className.replace(".", "/");
                        int index = path.lastIndexOf("/");
                        path = path.substring(0, index);
                        Enumeration<URL> urls = bundle.findEntries(path, "*", true);
                        while (urls.hasMoreElements()) {
                            LOG.info("Found {}", urls.nextElement());
                        }

                    }
                }
            }
        }
    }

    @Override
    public void unregisterBundle(final Bundle requestingBundle) {
        this.osgiClassLoader.removeBundle(requestingBundle);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object getService(final Bundle requestingBundle, final ServiceRegistration registration) {
        return null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void ungetService(final Bundle requestingBundle, final ServiceRegistration registration, final Object service) {
        // do nothing
    }

}
