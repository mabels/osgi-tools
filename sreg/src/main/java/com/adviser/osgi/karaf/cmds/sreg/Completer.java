package com.adviser.osgi.karaf.cmds.sreg;

import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.shell.console.completer.StringsCompleter;

public class Completer implements org.apache.karaf.shell.console.Completer {
    private ServiceRegistry serviceRegistry;

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter(new ArrayList<String>());
        return delegate.complete(buffer, cursor, candidates);
    }

}
