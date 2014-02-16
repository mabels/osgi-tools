package com.adviser.osgi.db.datasource.base;

import java.util.Dictionary;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Updated;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class DataSourceCfgFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceCfgFile.class);

    @Requires
    private com.adviser.osgi.db.datasource.DataSourceFactory dataSourceFactory;
    
    private Dictionary<String, String>  waitingForStartProps = null;
    private boolean waitingForStart = true;

    private final BundleContext context;
    
    private String pid = null;

    public DataSourceCfgFile(BundleContext context) {
        this.context = context;
    }

    @Updated
    public void updated(Dictionary<String, String> props) {
        if (waitingForStart) {
            waitingForStartProps = props;
        } else {
            pid = String.format("DS-%s", DataSourceFactory.getString("name", props));
            LOGGER.info("DataSourceCfgFactory:updated:{}", pid);
            dataSourceFactory.deleteDataSource(pid);
            props.put(Constants.SERVICE_PID, pid);
            pid = dataSourceFactory.createDataSource(props);
         }
    }

    @Validate
    public void start() {
        LOGGER.info("DataSourceCfgFactory:start:{}", pid);
        waitingForStart = false;
        updated(waitingForStartProps);
        waitingForStartProps = null;
    }

    @Invalidate
    public void stop() {
        LOGGER.info("DataSourceFactory:stop:{}", pid);
        dataSourceFactory.deleteDataSource(pid);
        waitingForStart = true;
    }

   
   
}
