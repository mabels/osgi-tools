package com.adviser.osgi.helper;

import java.util.Dictionary;

import org.apache.felix.ipojo.ComponentInstance;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IPOJOInstanceHelper {

    private static final Logger LOG = LoggerFactory.getLogger(IPOJOInstanceHelper.class);
    private final OSGiHelper osgiHelper;
    private final IPOJOHelper ipojoHelper;


    public IPOJOInstanceHelper(final BundleContext bundleContext) {
        this.ipojoHelper = new IPOJOHelper(bundleContext);
        this.osgiHelper = new OSGiHelper(bundleContext);
    }

    /**
     * Create an iPojo instance for a given class and a given instance name using the given properties.
     * 
     * @param instanceClass
     *            class of the instance to be instantiated
     * @param instanceName
     *            instance name
     * @param props
     *            properties to be used to instantiate the object
     * @returns {@link InstanceHolder} object holding the instance just created and the IPOJO description object of class
     *          {@link ComponentInstance}
     */
    public <I, T> InstanceHolder<I> create(final Class<I> interfaceClass, final String instanceClassName,
            final String instanceName, final Dictionary<String, Object> props) {
        if (instanceName != null) {
            props.put("instance.name", instanceName);
        }
        // Create, register and start an instance of class instanceClass
        ComponentInstance ci = this.ipojoHelper.createComponentInstance(instanceClassName, props);

        // get the just created instance object from the OSGI registry and return it
        final I instance = this.osgiHelper.getServiceObject(interfaceClass, "(instance.name=" + ci.getInstanceName() + ")");
        if (instance != null && interfaceClass.isInstance(instance)) {
            LOG.debug("Created instance of class '{}' with instance name '{}'successfully.", instanceClassName,
                    ci.getInstanceName());
        } else {
            LOG.warn("Creation of instance of class '{}' failed.", instanceClassName);
        }
        return new InstanceHolder<I>(instance, ci);
    }

    /**
     * Disposes the iPojo instance.
     */
    public <T> void dispose(InstanceHolder<T> holder) {
        if (holder != null ) {
            holder.dispose();
        }
    }


    public class InstanceHolder<T> {

        private final T instance;
        private ComponentInstance componentInstance;


        public InstanceHolder(final T instance, final ComponentInstance ci) {
            this.instance = instance;
            this.componentInstance = ci;
        }

        public T getInstance() {
            return this.instance;
        }

        public ComponentInstance getComponentInstance() {
            return this.componentInstance;
        }

        public void dispose() {
            if (this.componentInstance != null) {
                this.componentInstance.dispose();
                this.componentInstance = null;
            }
        }
    }
}
