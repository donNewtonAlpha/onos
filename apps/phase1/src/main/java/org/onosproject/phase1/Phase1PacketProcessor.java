package org.onosproject.phase1;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.*;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/**
 * Created by nick on 10/5/15.
 */
public class Phase1PacketProcessor implements PacketProcessor {

    static final Logger log = LoggerFactory.getLogger(Phase1Component.class);

    private PacketService packetService;
    private FlowRuleService flowRuleService;

    private final String DNS_API_URL = "http://prodmos.foundry.att.com/d2/sdn/pc/requests/validate";
    private final String BRG_AUTHENTICATION_API_URL = "http://prodmos.foundry.att.com/d2/sdn/simulation/brg/activate?macaddr=";

    private final String CHARSET = StandardCharsets.UTF_8.name();

    public Phase1PacketProcessor(FlowRuleService flowRuleService, PacketService packetService){
        this.flowRuleService = flowRuleService;
        this.packetService = packetService;
    }

    @Override
    public void process(PacketContext context) {

        if(context.isHandled()){
            return;
        }
        InboundPacket pkt = context.inPacket();

        ConnectPoint incomingPoint = pkt.receivedFrom();
        PortNumber incomingPort = incomingPoint.port();

        if(incomingPort.equals(Phase1Component.serverPort)){
            processServerPacket(context);
        }else if(incomingPort.equals(Phase1Component.oltPort)){
            processOltPacket(context);
        }

    }

    private void processServerPacket(PacketContext context){
        log.debug("Server packet detected");
        //Expecting : iplearning packet


        try {
            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();
            IPacket payload = ethPkt.getPayload();

            if (payload instanceof ARP) {
                return;
            }

            IPv4 ip4Packet = (IPv4) payload;
            IPacket layer4Packet = ip4Packet.getPayload();

            if (layer4Packet instanceof UDP) {
                // check if this is an IP learning packet
                UDP udpPacket = (UDP) layer4Packet;
                if (ip4Packet.getDestinationAddress() == Phase1Component.ipLearningIP.address().getIp4Address().toInt()){
                    if (udpPacket.getDestinationPort() == Phase1Component.ipLearningPort.toInt()){
                        //This is an IP learning packet
                        Data data = (Data) udpPacket.getPayload();
                        int vlanId = Byte.toUnsignedInt(data.getData()[Phase1Component.IP_LEARNING_LENGTH-2])*256 + Byte.toUnsignedInt(data.getData()[Phase1Component.IP_LEARNING_LENGTH-1]);
                        Phase1Component.foundCustomerIP(vlanId, Ip4Address.valueOf(ip4Packet.getSourceAddress()));
                    }
                }else if (ip4Packet.getDestinationAddress() == Phase1Component.uverseIpLearningIP.address().getIp4Address().toInt()){
                    if (udpPacket.getDestinationPort() == Phase1Component.ipLearningPort.toInt()){
                        //This is a Uverse IP learning packet
                        Data data = (Data) udpPacket.getPayload();
                        int vlanId = Byte.toUnsignedInt(data.getData()[Phase1Component.IP_LEARNING_LENGTH-2])*256 + Byte.toUnsignedInt(data.getData()[Phase1Component.IP_LEARNING_LENGTH-1]);
                        Phase1Component.foundUverseCustomerIP(vlanId, Ip4Address.valueOf(ip4Packet.getSourceAddress()));
                    }
                }


            }
        }catch (Exception e){
            log.error("Server Wan packet processing exception", e);

        }

    }

    private void processOltPacket(PacketContext context){
        log.debug("olt packet detected");
        //Expecting : DNS, IGMP, BRG detection

        try {
            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();
            IPacket payload = ethPkt.getPayload();

            if(payload instanceof IPv4) {

                IPv4 ip4Packet = (IPv4) payload;

                if (ip4Packet.getProtocol() == IPv4.PROTOCOL_IGMP) {
                    //IGMP
                    processIgmpPackets(context);
                    return;
                }

                IPacket layer4Packet = ip4Packet.getPayload();

                if (layer4Packet instanceof UDP) {
                    UDP udpPacket = (UDP) layer4Packet;
                    if (udpPacket.getDestinationPort() == 53) {

                        return;
                    }
                }
            }

            processBrgDetectionPacket(context);



        }catch (Exception e){
            log.error("Olt packet processing exception", e);
            log.debug(context.inPacket().parsed().toString());

            TrafficTreatment sendToServerLan = DefaultTrafficTreatment.builder().setOutput(Phase1Component.serverPort).build();
            OutboundPacket packetToSend = new DefaultOutboundPacket(Phase1Component.torId, sendToServerLan, context.inPacket().unparsed());

            packetService.emit(packetToSend);
        }
    }

    private void processBrgDetectionPacket(PacketContext context){

        Ethernet ethPkt = context.inPacket().parsed();

        MacAddress brgMac = ethPkt.getSourceMAC();

        if(!Phase1Component.brgDetectionFlows.containsKey(brgMac)){
            log.warn("Not a brg detection packet");
            return;
        }

        log.info("BRG detection packet : " + brgMac.toString());

        FlowRule toRemove = Phase1Component.brgDetectionFlows.get(brgMac);
        log.info(toRemove.toString());
        flowRuleService.removeFlowRules(toRemove);
        Phase1Component.brgDetectionFlows.remove(brgMac);

        log.info("BRG authentication : " + brgMac.toString());
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(BRG_AUTHENTICATION_API_URL);
        try{
            urlBuilder.append(URLEncoder.encode(brgMac.toString(), CHARSET));
        }catch(Exception e){
            log.error("Encoding error", e);
        }
        try{
            HttpURLConnection httpConnection = (HttpURLConnection) new URL(urlBuilder.toString()).openConnection();
            httpConnection.setRequestMethod("GET");
            int  status = httpConnection.getResponseCode();
            if(status < 200 || status >=300){
                //Error
                log.warn("API error, status code : " + status);
            }else{
                log.info("BRG init successful");
            }

        }catch (Exception e){
            log.error ("API exception", e);
        }

    }



    private void processIgmpPackets(PacketContext context) {

        log.info("IGMP packet caught");

        InboundPacket pkt = context.inPacket();
        Ethernet ethPkt = pkt.parsed();
        //IPv4 ip4Packet = (IPv4) ethPkt.getPayload();


        int vlanID = ethPkt.getCtagVlanID();

        if (Phase1Component.ipTvEnabledCustomers.containsKey(vlanID)&&Phase1Component.ipTvEnabledCustomers.get(vlanID)){
            //Uverse allowed

            /*ip4Packet.setSourceAddress(Phase1Component.customersUverseIP.get(vlanID).toInt());
            ethPkt.setVlanID(Phase1Component.outerTag.toShort(), (short) vlanID);
            ethPkt.resetChecksum();
*/
            TrafficTreatment sendToServerLan = DefaultTrafficTreatment.builder().setOutput(Phase1Component.serverPort).build();
            OutboundPacket packetToSend = new DefaultOutboundPacket(Phase1Component.torId, sendToServerLan, pkt.unparsed());

            log.debug("IGMP packet passed to the server");

            packetService.emit(packetToSend);

        }

    }

}
