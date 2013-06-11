package de.nextaudience.tools.accesslog2solr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ReadAccessLog {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReadAccessLog.class);

    public void openStream(Exchange ex) throws FileNotFoundException {
   //     final String fname = ex.getIn().getHeader(Exchange.FILE_LOCK_FILE_NAME, String.class);
        LOGGER.info("openStream:"+ ex.getIn().getBody(File.class).getAbsolutePath());
        ex.getOut().setHeader("stream", new FileInputStream(ex.getIn().getBody(File.class)));
    }
    public void read(Exchange ex) {
        LOGGER.info("read:"+ex.getIn().getBody());
    }
}
