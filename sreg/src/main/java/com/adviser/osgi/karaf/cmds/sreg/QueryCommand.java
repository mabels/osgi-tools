/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adviser.osgi.karaf.cmds.sreg;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.service.command.CommandSession;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;


@Command(scope = "sreg", name = "query", description = "Executes a EL service registry query")
public class QueryCommand implements Action {

    @Argument(index=0, name="clazz", required=true, description="Interface Name", multiValued=false)
    String clazz;

    @Argument(index=1, name="el", required=true, description="EL Query", multiValued=false)
    String el;

    private ServiceRegistry serviceRegistry;
    private BundleContext context;
    
    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
        
    }
    
    public void setContext(BundleContext context) {
        this.context = context;
        
    }

  
    @Override
    public Object execute(CommandSession session) throws Exception {
        System.out.println("Query:"+clazz+":"+el+"=>"+context+"=>"+serviceRegistry);
        ServiceReference[] serviceReferences = context.getAllServiceReferences(clazz, el);
        if (serviceReferences == null) {
            System.out.println("Nothing found!");
            return null;
        }
        for(ServiceReference sr : serviceReferences) {
            System.out.println("Found:"+context.getService(sr).getClass().getCanonicalName());
        }
        return null;
    }


}
