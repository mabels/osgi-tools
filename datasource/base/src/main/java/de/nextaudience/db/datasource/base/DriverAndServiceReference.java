package de.nextaudience.db.datasource.base;

import de.nextaudience.db.datasource.DriverFactory;
import org.osgi.framework.ServiceReference;

public class DriverAndServiceReference {

     public final DriverFactory driverFactory;
     public final ServiceReference<DriverFactory> serviceReference;

     public DriverAndServiceReference(DriverFactory driverFactory, ServiceReference<DriverFactory> serviceReference) {
         this.driverFactory = driverFactory;
         this.serviceReference = serviceReference;
     }
 }