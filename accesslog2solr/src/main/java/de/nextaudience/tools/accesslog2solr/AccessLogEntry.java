package de.nextaudience.tools.accesslog2solr;

import org.apache.camel.dataformat.bindy.annotation.CsvRecord;
import org.apache.camel.dataformat.bindy.annotation.DataField;


@CsvRecord( separator = " " )
public class AccessLogEntry {
    //127.0.0.1 - - [11/Jun/2013:06:13:33 +0200] \GET /ads?rt=8&et=12&i=31197&se=p&cs=1060&ev=Drakensang&aid=40&creative=33038383057&gclid=CJKm1PaR27cCFVDJtAoddAwAmg HTTP/1.1\ 302 180 \http://www.google.de/aclk?sa=l&ai=C-9mq66O2Ub3hL-aU0wWJj4HAC4m1nLsEmbTN7HrJuOusBwgAEAFQi7fhxgZglaLxgZAHoAHv0NzOA8gBAakChqij20I7tj6qBCJP0INKtMcIXWAg1l7zwoqcJpHiwL0jEjaiO5NQDh0RghmvgAf5rqMx&sig=AOD64_28ij9XzcqL-2ZRCPCERoAYhIgeyQ&ved=0CDMQ0Qw&adurl=http://akimu.bigpoint.com/ads%3Frt%3D8%26et%3D12%26i%3D31197%26se%3Dp%26cs%3D1060%26ev%3DDrakensang%26aid%3D40%26creative%3D33038383057&rct=j&q=drakensang \Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.2; WOW64; Trident/6.0; MAARJS)
    @DataField(pos = 1)
    private String ip;
    @DataField(pos = 4)
    private String dateTime;
    @DataField(pos = 5)
    private String timeZone;
    @DataField(pos = 6)
    private String method;
    @DataField(pos = 7)
    private String pathQuery;
    @DataField(pos = 8)
    private String protocol;
    @DataField(pos = 9)
    private String retcode;
    @DataField(pos = 10)
    private String size;
    @DataField(pos = 11)
    private String referer;

}
