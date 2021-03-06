package com.adviser.osgi.db.datasource.test;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    @Override
    public void start(BundleContext context) throws Exception {
        LOGGER.info("start com.adviser.osgi.db.datasource.test");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        LOGGER.info("stop com.adviser.osgi.db.datasource.test");
    }
}
