package de.nextaudience.tools.accesslog2solr;

import org.apache.camel.CamelContext;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.stream.StreamComponent;
import org.apache.camel.dataformat.bindy.csv.BindyCsvDataFormat;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.DataFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AccessLog2Solr {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessLog2Solr.class);


    private AccessLog2Solr() {
    }

    public static void main(String[] args) {
        final CamelContext camelContext = new DefaultCamelContext();
        camelContext.disableJMX();
        StreamComponent streamComponent = new StreamComponent();
        camelContext.addComponent("stream", streamComponent);
        try {
            camelContext.addRoutes(new RouteBuilder() {

                @Override
                public void configure() throws Exception {
                    LOGGER.info("AccessLog2Solr::RouteBuilder::configure");
                    DataFormat bindy = new BindyCsvDataFormat(AccessLogEntry.class);
                    from("file:///tmp/files?noop=true").
//                    unmarshal(bindy).
                     bean(ReadAccessLog.class, "tailFile");
                }

            });
            LOGGER.info("AccessLog2Solr:main:starting");
            camelContext.start();
            LOGGER.info("AccessLog2Solr:main:started");
            Thread.sleep(100000000000L);
        } catch (Exception e) {
            LOGGER.error("server:camelContext", e);
        }
    }

}