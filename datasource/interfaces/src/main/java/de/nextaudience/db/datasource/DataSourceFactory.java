package de.nextaudience.db.datasource;

import java.util.Dictionary;

public interface DataSourceFactory {

    public String createDataSource(String name, String url, String user, String password);
    
    public String createDataSource(Dictionary<String, String> props);

    public void deleteDataSource(String pid);


}
