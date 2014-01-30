package de.nextaudience.db.datasource;

import java.sql.SQLException;
import java.util.Dictionary;

public interface DataSourceFactory {

    public String createDataSource(Dictionary<String, String> props);

    public void deleteDataSource(String pid);


	/*
	  * databaseName
	  * systemDataSourceName
	*/
	public String createdatabaseFromName(Dictionary<String, String> props) throws SQLException;

	void dropdatabase(String pid) throws SQLException;


}
