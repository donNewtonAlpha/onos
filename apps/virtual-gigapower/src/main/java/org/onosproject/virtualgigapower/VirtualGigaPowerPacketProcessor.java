package org.onosproject.virtualgigapower;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.*;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by nick on 10/5/15.
 */
public class VirtualGigaPowerPacketProcessor implements PacketProcessor {

    static final Logger log = LoggerFactory.getLogger(VirtualGigaPowerPacketProcessor.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    private final String DNS_API_URL = "http://prodmos.foundry.att.com/d2/sdn/pc/requests/validate";
    private final String CHARSET = StandardCharsets.UTF_8.name();

    @Override
    public void process(PacketContext context) {

        if(context.isHandled()){
            return;
        }
        InboundPacket pkt = context.inPacket();

        ConnectPoint incomingPoint = pkt.receivedFrom();
        PortNumber incomingPort = incomingPoint.port();


    }


}
