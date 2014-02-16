package com.adviser.osgi.db.datasource.base;

import com.adviser.osgi.db.datasource.DriverFactory;
import org.osgi.framework.ServiceReference;

public class DriverAndServiceReference {

     public final DriverFactory driverFactory;
     public final ServiceReference<DriverFactory> serviceReference;

     public DriverAndServiceReference(DriverFactory driverFactory, ServiceReference<DriverFactory> serviceReference) {
         this.driverFactory = driverFactory;
         this.serviceReference = serviceReference;
     }
 }
