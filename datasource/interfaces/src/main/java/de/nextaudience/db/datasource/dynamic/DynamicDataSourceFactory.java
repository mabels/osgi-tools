package de.nextaudience.db.datasource.dynamic;


public interface DynamicDataSourceFactory {

    void createDataSource(String pid, String name, String url, String user, String password) throws Exception;
    void deleteDataSource(String pid, String url);
}
