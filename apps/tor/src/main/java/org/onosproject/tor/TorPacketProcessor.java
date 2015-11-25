package org.onosproject.tor;

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
public class TorPacketProcessor implements PacketProcessor {

    static final Logger log = LoggerFactory.getLogger(TorComponent.class);

    private PacketService packetService;
    private FlowRuleService flowRuleService;

    private final String DNS_API_URL = "http://prodmos.foundry.att.com/d2/sdn/pc/requests/validate";
    private final String BRG_AUTHENTICATION_API_URL = "http://prodmos.foundry.att.com/d2/sdn/simulation/brg/activate?macaddr=";

    private final String CHARSET = StandardCharsets.UTF_8.name();

    public TorPacketProcessor(FlowRuleService flowRuleService, PacketService packetService){
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

        if(incomingPort.equals(TorComponent.serverPort)){
            processServerPacket(context);
        }else if(incomingPort.equals(TorComponent.oltPort)){
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
                if (ip4Packet.getDestinationAddress() == TorComponent.ipLearningIP.address().getIp4Address().toInt()){
                    if (udpPacket.getDestinationPort() == TorComponent.ipLearningPort.toInt()){
                        //This is an IP learning packet
                        Data data = (Data) udpPacket.getPayload();
                        int vlanId = Byte.toUnsignedInt(data.getData()[TorComponent.IP_LEARNING_LENGTH-2])*256 + Byte.toUnsignedInt(data.getData()[TorComponent.IP_LEARNING_LENGTH-1]);
                        TorComponent.foundCustomerIP(vlanId, Ip4Address.valueOf(ip4Packet.getSourceAddress()));
                    }
                }else if (ip4Packet.getDestinationAddress() == TorComponent.uverseIpLearningIP.address().getIp4Address().toInt()){
                    if (udpPacket.getDestinationPort() == TorComponent.ipLearningPort.toInt()){
                        //This is a Uverse IP learning packet
                        Data data = (Data) udpPacket.getPayload();
                        int vlanId = Byte.toUnsignedInt(data.getData()[TorComponent.IP_LEARNING_LENGTH-2])*256 + Byte.toUnsignedInt(data.getData()[TorComponent.IP_LEARNING_LENGTH-1]);
                        TorComponent.foundUverseCustomerIP(vlanId, Ip4Address.valueOf(ip4Packet.getSourceAddress()));
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
                        processDnsPacket(context);
                        return;
                    }
                }
            }

            processBrgDetectionPacket(context);



        }catch (Exception e){
            log.error("Olt packet processing exception", e);
            log.debug(context.inPacket().parsed().toString());

            TrafficTreatment sendToServerLan = DefaultTrafficTreatment.builder().setOutput(TorComponent.serverPort).build();
            OutboundPacket packetToSend = new DefaultOutboundPacket(TorComponent.torId, sendToServerLan, context.inPacket().unparsed());

            packetService.emit(packetToSend);
        }
    }

    private void processBrgDetectionPacket(PacketContext context){

        Ethernet ethPkt = context.inPacket().parsed();

        MacAddress brgMac = ethPkt.getSourceMAC();

        if(!TorComponent.brgDetectionFlows.containsKey(brgMac)){
            log.warn("Not a brg detection packet");
            return;
        }

        log.info("BRG detection packet : " + brgMac.toString());

        FlowRule toRemove = TorComponent.brgDetectionFlows.get(brgMac);
        log.info(toRemove.toString());
        flowRuleService.removeFlowRules(toRemove);
        TorComponent.brgDetectionFlows.remove(brgMac);

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

    private void processDnsPacket(PacketContext context) {
        log.debug("DNS packet  caught");
        try {
            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();
            IPv4 ip4Packet = (IPv4) ethPkt.getPayload();

            int sockId = ethPkt.getStagVlanID();
            int vlanID = ethPkt.getCtagVlanID();

            if(TorComponent.dnsEnabledCustomers.containsKey(vlanID)){
                if(TorComponent.dnsEnabledCustomers.get(vlanID)){

                    MacAddress deviceMac = ethPkt.getSourceMAC();
                    IpAddress deviceIP = Ip4Address.valueOf(ip4Packet.getSourceAddress());

                    UDP udpPacket = (UDP) ip4Packet.getPayload();

                    byte[] request = udpPacket.getPayload().serialize();

                    ExecutorService ex = Executors.newSingleThreadExecutor();
                    Future<byte[]> future = ex.submit(DnsProxy.newResolution(request));

                    String url = "exception";
                    try {
                        url = DnsProxy.extractDomainNameFromRequest(request);
                    } catch (Exception e) {
                        log.error("exception dns resolution : ", e);
                    }
                    //log.info(" DNS request : " + url);

                    //Call parental control API
                    //sockId, vlanId, deviceMac, url

                    StringBuilder  urlBuilder = new StringBuilder();

                    urlBuilder.append(DNS_API_URL);
                    urlBuilder.append("?vlanId=");
                    urlBuilder.append(vlanID);
                    urlBuilder.append("&sockId=");
                    urlBuilder.append(sockId);
                    urlBuilder.append("&searchVal=");
                    urlBuilder.append(URLEncoder.encode(url, CHARSET));
                    urlBuilder.append("&macAddr=");
                    urlBuilder.append(URLEncoder.encode(deviceMac.toString(), CHARSET));


                    String requestUrl = urlBuilder.toString();

                    log.info("URL : " + requestUrl);


                    boolean allowed = true;
                    String redirect="";

                    try{
                        HttpURLConnection httpConnection = (HttpURLConnection) new URL(requestUrl).openConnection();
                        httpConnection.setRequestMethod("GET");
                        int  status = httpConnection.getResponseCode();
                        if(status < 200 || status >=300){
                            //Error
                            log.info("API error : " + status);
                        }else{
                            InputStream is = httpConnection.getInputStream();
                            JsonReader reader = Json.createReader(is);
                            JsonObject json = reader.readObject();

                            allowed = json.getBoolean("allowed");
                            redirect = json.getJsonString("bannedUrlRedirect").toString();

                            reader.close();
                            is.close();
                        }

                    }catch (Exception e){
                        log.error ("API exception", e);
                    }

                    //Unrestricted DNS

                    byte[] response;
                    try {
                        response = future.get(5000, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        log.error("future exception", e);
                        return;
                    }
                    //debug
                    StringBuilder dnsResponseData = new StringBuilder("Raw DNS response");{
                        for(int i = 0; i < response.length; i++){
                            dnsResponseData.append(response[i]);
                            dnsResponseData.append(' ');
                        }
                    }
                    log.debug(dnsResponseData.toString());


                    if(!allowed){
                        log.info("domain not allowed");
                        try{
                            DnsProxy.switchResponse(response,redirect);
                        }catch (Exception e){
                            log.error("Switch Response exception" , e);
                            return;
                        }
                    }

                    log.info("DNS response retrieved");




                    //Recomposition of a new packet
                    Data responseData = new Data(response);

                    UDP responseUdp = new UDP();

                    responseUdp.setPayload(responseData);
                    responseData.setParent(responseUdp);

                    responseUdp.setSourcePort(udpPacket.getDestinationPort());
                    responseUdp.setDestinationPort(udpPacket.getSourcePort());
                    //responseUdp.resetChecksum();

                    IPv4 responseIp = new IPv4();

                    responseIp.setPayload(responseUdp);
                    responseUdp.setParent(responseIp);

                    responseIp.setSourceAddress(ip4Packet.getDestinationAddress());
                    responseIp.setDestinationAddress(ip4Packet.getSourceAddress());
                    responseIp.setIdentification(ip4Packet.getIdentification());
                    responseIp.setDscp(ip4Packet.getDscp());
                    responseIp.setProtocol(ip4Packet.getProtocol());
                    responseIp.setTtl((byte) 20);
                    //responseIp.resetChecksum();

                    Ethernet responseEthernet = new Ethernet();

                    responseEthernet.setPayload(responseIp);
                    responseIp.setParent(responseEthernet);

                    responseEthernet.setDestinationMACAddress(ethPkt.getSourceMAC());
                    responseEthernet.setSourceMACAddress(ethPkt.getDestinationMAC());
                    responseEthernet.setEtherType(ethPkt.getEtherType());
                    responseEthernet.setVlanID(Ethernet.TYPE_VLAN, ethPkt.getStagVlanID(), Ethernet.TYPE_VLAN, ethPkt.getCtagVlanID());
                    responseEthernet.resetChecksum();




                    TrafficTreatment.Builder dnsIpSpoofing = DefaultTrafficTreatment.builder();
                    dnsIpSpoofing.setOutput(TorComponent.oltPort);
                    OutboundPacket outboundPacket = new DefaultOutboundPacket(TorComponent.torId, dnsIpSpoofing.build(), ByteBuffer.wrap(responseEthernet.serialize()));

                    packetService.emit(outboundPacket);
                    //debug
                    StringBuilder buffer = new StringBuilder("DNS message sent, ");


                    buffer.append((" bytes : "));
                    for(int i =0; i< response.length; i++){
                        buffer.append(response[i]);
                        buffer.append(' ');
                    }

                    log.info(buffer.toString());
                    return;
                }
            }

            //The customer does  not have this service enabled
            // reinject the packet to the server
            TrafficTreatment sendToServerLan = DefaultTrafficTreatment.builder().setOutput(TorComponent.serverPort).build();
            OutboundPacket packetToSend = new DefaultOutboundPacket(TorComponent.torId, sendToServerLan, pkt.unparsed());

            packetService.emit(packetToSend);


        } catch (Exception e) {
            log.error("exception during DNS", e);
        }

    }

    private void processIgmpPackets(PacketContext context) {

        log.info("IGMP packet caught");

        InboundPacket pkt = context.inPacket();
        Ethernet ethPkt = pkt.parsed();
        //IPv4 ip4Packet = (IPv4) ethPkt.getPayload();


        int vlanID = ethPkt.getCtagVlanID();

        if (TorComponent.ipTvEnabledCustomers.containsKey(vlanID)&&TorComponent.ipTvEnabledCustomers.get(vlanID)){
            //Uverse allowed

            /*ip4Packet.setSourceAddress(TorComponent.customersUverseIP.get(vlanID).toInt());
            ethPkt.setVlanID(TorComponent.outerTag.toShort(), (short) vlanID);
            ethPkt.resetChecksum();
*/
            TrafficTreatment sendToServerLan = DefaultTrafficTreatment.builder().setOutput(TorComponent.serverPort).build();
            OutboundPacket packetToSend = new DefaultOutboundPacket(TorComponent.torId, sendToServerLan, pkt.unparsed());

            log.debug("IGMP packet passed to the server");

            packetService.emit(packetToSend);

        }

    }

}
