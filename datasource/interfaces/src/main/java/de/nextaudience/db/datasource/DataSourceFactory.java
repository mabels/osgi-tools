package de.nextaudience.db.datasource;

import java.util.Dictionary;

public interface DataSourceFactory {

    public String createDataSource(Dictionary<String, String> props);

    public void deleteDataSource(String pid);


}
