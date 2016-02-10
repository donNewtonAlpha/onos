
package org.onosproject.phase1;

import org.apache.felix.scr.annotations.*;



import org.onlab.packet.*;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.apps.TorService;
import org.onosproject.net.flow.*;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.*;
import org.onosproject.net.packet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.*;


import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;


/**
 * Created by nick on 6/26/15.
 */
@Component(immediate = true)
@Service
public class Phase1Component{


    static final Logger log = LoggerFactory.getLogger(Phase1Component.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;


    static ApplicationId appId;
    static final DeviceId torId = DeviceId.deviceId("of:000000000000da7a");

    static final int ACL_TABLE = 60;
    static final int PORT_TABLE = 0;
    static final int VLAN_TABLE = 10;
    static final int TMAC_TABLE = 20;
    static final int VLAN1_TABLE = 11;
    static final int BRIDGING_TABLE = 50;

    static final int IP_LEARNING_LENGTH = 50;

    private static final String CONNECT = "connectInternet";
    private static final String DISCONNECT = "disconnectInternet";
    private static final String DNS_SERVICE = "dnsIntercept";
    private static final String NO_DNS_SERVICE = "noDnsIntercept";
    private static final String CONNECT_TV = "connectUverse";
    private static final String DISCONNECT_TV = "disconnectUverse";
    private static final String BRG_DETECTION = "brgDetection";

    private static final byte[] HTTP_OK = "HTTP/1.1 200 OK".getBytes();


    // Physical port 1, port splitting
    static final PortNumber serverPort = PortNumber.portNumber(1);
    //static final PortNumber uversePort = PortNumber.portNumber(2);
    static final PortNumber internetPort = PortNumber.portNumber(2);
    // Physical port 2
    static final PortNumber oltPort = PortNumber.portNumber(3);




    static final VlanId outerTag = VlanId.vlanId((short) 5);
    static final VlanId tunnelVlan = VlanId.vlanId((short) 555);
    static final VlanId internetVlan = VlanId.vlanId((short) 500);
    static final VlanId uverseVlan = VlanId.vlanId((short) 696);
    static final VlanId vlan0 = VlanId.vlanId((short) 1);


    static HashMap<Integer, Ip4Address> customersIP = new HashMap<>();
    static HashMap<Integer, Ip4Address> customersUverseIP = new HashMap<>();
    private static HashMap<Integer, FlowRule> outgoingInternetFlows = new HashMap<>();
    private static HashMap<Integer, FlowRule> incomingInternetFlows = new HashMap<>();
    private static HashMap<Integer, FlowRule> outgoingUverseFlows = new HashMap<>();
    static HashMap<MacAddress, FlowRule> brgDetectionFlows = new HashMap<>();
    static HashMap<Integer, Boolean> dnsEnabledCustomers = new HashMap<>();
    static HashMap<Integer, Boolean> ipTvEnabledCustomers = new HashMap<>();


    static Ip4Prefix ipLearningIP = Ip4Prefix.valueOf("207.143.192.88/32");
    static Ip4Prefix uverseIpLearningIP = Ip4Prefix.valueOf("10.225.9.254/32");
    static Ip4Prefix ipLearningOrigin = Ip4Prefix.valueOf("192.168.1.2/32");
    static TpPort ipLearningPort = TpPort.tpPort(6789);


    private static final VlanId oltMulticastVlan = VlanId.vlanId((short) 4001);


    private final byte NO_FRAGMENT = (byte) 2;
    private final short DEFAULT_ID = (short) 1111;
    private final byte DEFAULT_TTL = 64;
    private final MacAddress DEFAULT_SOURCE_MAC = MacAddress.valueOf(123456789);
    private final MacAddress VSG_MAC = MacAddress.valueOf("76:eb:27:f6:47:f1");



    private static final int JSON_COMMUNICATION_PORT = 6493;


    private static FlowRule uverseBlockingFlow;

    private static Semaphore lock = new Semaphore(1);

    @Activate
    protected void activate() {

        log.debug("trying to activate");
        appId = coreService.registerApplication("org.onosproject.tor");

        packetService.addProcessor(new Phase1PacketProcessor(flowRuleService, packetService), 1);



        //Olt to vSGs server two way flow
        oltToServerBidirectionnal(outerTag, torId);

        //Dhcp for the vSGs WAN interfaces
        //dhcpFlows(vlan0, internetPort);
        //dhcpFlows(uverseVlan, uversePort);
        //Default ARP flow
        //arpFlows(vlan0, internetPort);
        //arpFlows(uverseVlan, uversePort);
        //UverseFeed to the Olt, bypassing the vSGs
        //uverseTraffic();

        //Packets capture  to controller
        //dnsCapture();
        //igmpCapture();
        ipLearningCapture(serverPort);
        //ipLearningCapture(oltPort); // For testing, not for the real system

        //newBrgDetectionCapture(MacAddress.valueOf("C4:6E:1F:C5:7A:8B"));

        //Listening for API calls
        listen();




    }


    @Deactivate
    protected void deactivate() {

        flowRuleService.removeFlowRulesById(appId);


        Iterable<Group> appGroups = groupService.getGroups(torId, appId);
        for(Group group : appGroups) {
            groupService.removeGroup(group.deviceId(), group.appCookie(), group.appId());
        }

        log.info("Stopped");
    }

    private void ipLearningCapture(PortNumber port){

        //FLow for customer IP learning

        TrafficSelector.Builder ipLearningSelector =  DefaultTrafficSelector.builder();
        ipLearningSelector.matchInPort(port);
        ipLearningSelector.matchVlanId(vlan0);
        ipLearningSelector.matchEthType(Ethernet.TYPE_IPV4);
        ipLearningSelector.matchIPProtocol(IPv4.PROTOCOL_UDP);
        ipLearningSelector.matchIPDst(ipLearningIP);
        ipLearningSelector.matchUdpDst(ipLearningPort);

        FlowRule.Builder ipLearningRule = DefaultFlowRule.builder();
        ipLearningRule.withSelector(ipLearningSelector.build());
        ipLearningRule.withTreatment(DefaultTrafficTreatment.builder().punt().build());
        ipLearningRule.withPriority(49300);
        ipLearningRule.fromApp(appId);
        ipLearningRule.forTable(ACL_TABLE);
        ipLearningRule.makePermanent();
        ipLearningRule.forDevice(torId);

        flowRuleService.applyFlowRules(ipLearningRule.build());


        TrafficSelector.Builder uverseIpLearningSelector =  DefaultTrafficSelector.builder();
        uverseIpLearningSelector.matchInPort(port);
        uverseIpLearningSelector.matchVlanId(uverseVlan);
        uverseIpLearningSelector.matchEthType(Ethernet.TYPE_IPV4);
        uverseIpLearningSelector.matchIPProtocol(IPv4.PROTOCOL_UDP);
        uverseIpLearningSelector.matchIPDst(uverseIpLearningIP);
        uverseIpLearningSelector.matchUdpDst(ipLearningPort);

        FlowRule.Builder uverseIpLearningRule = DefaultFlowRule.builder();
        uverseIpLearningRule.withSelector(uverseIpLearningSelector.build());
        uverseIpLearningRule.withTreatment(DefaultTrafficTreatment.builder().punt().build());
        uverseIpLearningRule.withPriority(49301);
        uverseIpLearningRule.fromApp(appId);
        uverseIpLearningRule.forTable(ACL_TABLE);
        uverseIpLearningRule.makePermanent();
        uverseIpLearningRule.forDevice(torId);

        flowRuleService.applyFlowRules(uverseIpLearningRule.build());

    }

    void newBrgDetectionCapture(MacAddress brgMac){

        //Flow to "authenticate" a new BRG

        TrafficSelector.Builder newBrgSelector = DefaultTrafficSelector.builder();
        newBrgSelector.matchInPort(oltPort);
        newBrgSelector.matchEthSrc(brgMac);

        FlowRule.Builder newBrgDetectionRule =  DefaultFlowRule.builder();
        newBrgDetectionRule.withSelector(newBrgSelector.build());
        newBrgDetectionRule.withTreatment(DefaultTrafficTreatment.builder().punt().build());
        newBrgDetectionRule.fromApp(appId);
        newBrgDetectionRule.withPriority(49500);
        newBrgDetectionRule.makePermanent();
        newBrgDetectionRule.forTable(ACL_TABLE);
        newBrgDetectionRule.forDevice(torId);

        FlowRule brgDetectionFlow = newBrgDetectionRule.build();
        brgDetectionFlows.put(brgMac, brgDetectionFlow);

        flowRuleService.applyFlowRules(brgDetectionFlow);

    }



    private void untaggedPacketsTagging(PortNumber port, VlanId vlanId){

        vlanTableHackFlows(port,vlanId);

        TrafficSelector.Builder taggingSelector = DefaultTrafficSelector.builder();
        taggingSelector.matchInPort(port);
        taggingSelector.matchVlanId(VlanId.NONE);

        TrafficTreatment.Builder taggingTreatment = DefaultTrafficTreatment.builder();
        taggingTreatment.setVlanId(vlanId);
        taggingTreatment.transition(TMAC_TABLE);

        FlowRule.Builder taggingRule = DefaultFlowRule.builder();
        taggingRule.withSelector(taggingSelector.build());
        taggingRule.withTreatment(taggingTreatment.build());
        taggingRule.makePermanent();
        taggingRule.withPriority((short) port.toLong());
        taggingRule.fromApp(appId);
        taggingRule.forDevice(torId);
        taggingRule.forTable(VLAN_TABLE);

        flowRuleService.applyFlowRules(taggingRule.build());



    }


    private void oltToServerBidirectionnal(VlanId vlanId, DeviceId deviceId){


        vlanTableHackFlows(oltPort, vlanId);
        vlanTableHackFlows(serverPort, vlanId);

        TrafficSelector.Builder OltToServerSelector = DefaultTrafficSelector.builder();
        OltToServerSelector.matchInPort(oltPort);
        OltToServerSelector.matchVlanId(vlanId);

        //ACL Table flow for olt to  server

        TrafficTreatment.Builder outputServerLan = DefaultTrafficTreatment.builder();
        outputServerLan.group(GroupFinder.getL2Interface(serverPort, vlanId, deviceId));


        FlowRule.Builder oltToServer = DefaultFlowRule.builder();
        oltToServer.withSelector(OltToServerSelector.build());
        oltToServer.withTreatment(outputServerLan.build());
        oltToServer.withPriority(41003);
        oltToServer.fromApp(appId);
        oltToServer.forTable(ACL_TABLE);
        oltToServer.makePermanent();
        oltToServer.forDevice(deviceId);

        flowRuleService.applyFlowRules(oltToServer.build());



        //Flow from the extended LAN to the OLT (ACL)


        TrafficSelector.Builder toOltSelector = DefaultTrafficSelector.builder();
        toOltSelector.matchInPort(serverPort);
        toOltSelector.matchVlanId(vlanId);

        TrafficTreatment.Builder toOltGroupTreatment = DefaultTrafficTreatment.builder();
        toOltGroupTreatment.group(GroupFinder.getL2Interface(oltPort, vlanId, deviceId));



        FlowRule.Builder serverToOlt = DefaultFlowRule.builder();
        serverToOlt.withSelector(toOltSelector.build());
        serverToOlt.withTreatment(toOltGroupTreatment.build());
        serverToOlt.withPriority(41002);
        serverToOlt.fromApp(appId);
        serverToOlt.forTable(ACL_TABLE);
        serverToOlt.makePermanent();
        serverToOlt.forDevice(deviceId);

        flowRuleService.applyFlowRules(serverToOlt.build());


    }




    private void vlanTableHackFlows(PortNumber port, VlanId vlanId){

        TrafficSelector.Builder vlanTableHackSelector = DefaultTrafficSelector.builder();
        vlanTableHackSelector.matchInPort(port);
        vlanTableHackSelector.matchVlanId(vlanId);

        TrafficTreatment.Builder vlanTableHackTreatment = DefaultTrafficTreatment.builder();
        vlanTableHackTreatment.transition(TMAC_TABLE);

        FlowRule.Builder vlanTableHackRule = DefaultFlowRule.builder();
        vlanTableHackRule.withSelector(vlanTableHackSelector.build());
        vlanTableHackRule.withTreatment(vlanTableHackTreatment.build());
        vlanTableHackRule.withPriority(8);
        vlanTableHackRule.forTable(VLAN_TABLE);
        vlanTableHackRule.fromApp(appId);
        vlanTableHackRule.forDevice(torId);
        vlanTableHackRule.makePermanent();

        flowRuleService.applyFlowRules(vlanTableHackRule.build());

    }

    /*private  void dhcpFlows(VlanId vlan, PortNumber port){

        TrafficSelector.Builder dhcpRequestSelector = DefaultTrafficSelector.builder();
        dhcpRequestSelector.matchInPort(serverPort);
        dhcpRequestSelector.matchVlanId(vlan);
        dhcpRequestSelector.matchEthType(Ethernet.TYPE_IPV4);
        dhcpRequestSelector.matchIPProtocol(IPv4.PROTOCOL_UDP);
        dhcpRequestSelector.matchUdpDst(TpPort.tpPort(67));

        TrafficTreatment.Builder dhcpRequestTreatment = DefaultTrafficTreatment.builder();
        if(port.equals(internetPort)) {
            dhcpRequestTreatment.group(internetGroup);
        }else{
            dhcpRequestTreatment.group(uverseGroup);
        }
        FlowRule.Builder dhcpRequestRule = DefaultFlowRule.builder();
        dhcpRequestRule.withSelector(dhcpRequestSelector.build());
        dhcpRequestRule.withTreatment(dhcpRequestTreatment.build());
        dhcpRequestRule.withPriority(50001);
        dhcpRequestRule.makePermanent();
        dhcpRequestRule.forTable(ACL_TABLE);
        dhcpRequestRule.fromApp(appId);
        dhcpRequestRule.forDevice(torId);


        TrafficSelector.Builder dhcpResponseSelector = DefaultTrafficSelector.builder();
        dhcpResponseSelector.matchInPort(port);
        dhcpResponseSelector.matchEthType(Ethernet.TYPE_IPV4);
        dhcpResponseSelector.matchIPProtocol(IPv4.PROTOCOL_UDP);
        dhcpResponseSelector.matchUdpDst(TpPort.tpPort(68));

        TrafficTreatment.Builder dhcpResponseTreatment = DefaultTrafficTreatment.builder();
        if(port.equals(internetPort)) {
            dhcpResponseTreatment.group(serverWanGroup);
        }else{
            dhcpResponseTreatment.group(serverUverseGroup);
        }
        FlowRule.Builder dhcpResponseRule = DefaultFlowRule.builder();
        dhcpResponseRule.withSelector(dhcpResponseSelector.build());
        dhcpResponseRule.withTreatment(dhcpResponseTreatment.build());
        dhcpResponseRule.withPriority(50002);
        dhcpResponseRule.makePermanent();
        dhcpResponseRule.forTable(ACL_TABLE);
        dhcpResponseRule.fromApp(appId);
        dhcpResponseRule.forDevice(torId);

        flowRuleService.applyFlowRules(dhcpRequestRule.build(), dhcpResponseRule.build());

    }

    private  void arpFlows(VlanId vlan, PortNumber port){

        TrafficSelector.Builder arpFromServerSelector = DefaultTrafficSelector.builder();
        arpFromServerSelector.matchInPort(serverPort);
        arpFromServerSelector.matchEthType(Ethernet.TYPE_ARP);
        arpFromServerSelector.matchVlanId(vlan);


        TrafficTreatment.Builder arpFromServerTreatment = DefaultTrafficTreatment.builder();
        if(port.equals(internetPort)) {
            arpFromServerTreatment.group(internetGroup);
        }else{
            arpFromServerTreatment.group(uverseGroup);
        }

        FlowRule.Builder arpFromServerRule = DefaultFlowRule.builder();
        arpFromServerRule.withSelector(arpFromServerSelector.build());
        arpFromServerRule.withTreatment(arpFromServerTreatment.build());
        arpFromServerRule.withPriority(50003);
        arpFromServerRule.makePermanent();
        arpFromServerRule.forTable(ACL_TABLE);
        arpFromServerRule.fromApp(appId);
        arpFromServerRule.forDevice(torId);


        TrafficSelector.Builder arpFromInternetSelector = DefaultTrafficSelector.builder();
        arpFromInternetSelector.matchInPort(port);
        arpFromInternetSelector.matchEthType(Ethernet.TYPE_ARP);



        TrafficTreatment.Builder arpFromInternetTreatment = DefaultTrafficTreatment.builder();
        if(port.equals(internetPort)) {
            arpFromInternetTreatment.group(serverWanGroup);
        }else{
            arpFromInternetTreatment.group(serverUverseGroup);
        }
        FlowRule.Builder arpFromInternetRule = DefaultFlowRule.builder();
        arpFromInternetRule.withSelector(arpFromInternetSelector.build());
        arpFromInternetRule.withTreatment(arpFromInternetTreatment.build());
        arpFromInternetRule.withPriority(50004);
        arpFromInternetRule.makePermanent();
        arpFromInternetRule.forTable(ACL_TABLE);
        arpFromInternetRule.fromApp(appId);
        arpFromInternetRule.forDevice(torId);

        flowRuleService.applyFlowRules(arpFromServerRule.build(), arpFromInternetRule.build());

    }
*/




    public void  connectInternet(int vlanId){

        log.info("Connecting customer : " + vlanId);

        //Check if the flows already exists
        if(outgoingInternetFlows.containsKey(vlanId) && incomingInternetFlows.containsKey(vlanId)){
            //flows already exist
            return;
        }
        int i =0;
        while (!customersIP.containsKey(vlanId)){
            detectCustomerIP(vlanId, ipLearningIP);
            i++;
            try {
                Thread.sleep(1500);
            }catch (Exception e){
                log.error("connectInternet sleeping exception", e);
            }
            if(i > 20){
                log.error("Was not able to connect customer : "+ vlanId);
                return;
            }
        }

        log.info("Customer's IP known : " + customersIP.get(vlanId).toString());




        TrafficSelector.Builder outgoingSelector = DefaultTrafficSelector.builder();
        outgoingSelector.matchInPort(serverPort);
        outgoingSelector.matchVlanId(internetVlan);
        outgoingSelector.matchEthType(Ethernet.TYPE_IPV4);
        outgoingSelector.matchIPSrc(customersIP.get(vlanId).toIpPrefix());

        TrafficTreatment.Builder outgoingTreatment = DefaultTrafficTreatment.builder();
        //outgoingTreatment.group(internetGroup);

        FlowRule.Builder outgoingRule = DefaultFlowRule.builder();
        outgoingRule.withSelector(outgoingSelector.build());
        outgoingRule.withTreatment(outgoingTreatment.build());
        outgoingRule.withPriority(42001);
        outgoingRule.fromApp(appId);
        outgoingRule.forTable(ACL_TABLE);
        outgoingRule.makePermanent();
        outgoingRule.forDevice(torId);

        FlowRule outgoingFlow = outgoingRule.build();

        outgoingInternetFlows.remove(vlanId);
        outgoingInternetFlows.put(vlanId, outgoingFlow);

        TrafficSelector.Builder incomingSelector = DefaultTrafficSelector.builder();
        incomingSelector.matchInPort(internetPort);
        incomingSelector.matchVlanId(internetVlan);
        incomingSelector.matchEthType(Ethernet.TYPE_IPV4);
        incomingSelector.matchIPDst(customersIP.get(vlanId).toIpPrefix());

        TrafficTreatment.Builder incomingTreatment = DefaultTrafficTreatment.builder();
        //incomingTreatment.group(serverWanRewriteGroup);

        FlowRule.Builder incomingRule = DefaultFlowRule.builder();
        incomingRule.withSelector(incomingSelector.build());
        incomingRule.withTreatment(incomingTreatment.build());
        incomingRule.withPriority(42002);
        incomingRule.fromApp(appId);
        incomingRule.forTable(ACL_TABLE);
        incomingRule.makePermanent();
        incomingRule.forDevice(torId);

        FlowRule incomingFlow = incomingRule.build();

        incomingInternetFlows.remove(vlanId);
        incomingInternetFlows.put(vlanId, incomingFlow);

        flowRuleService.applyFlowRules(outgoingFlow, incomingFlow);
    }

    public void disconnectInternet(int vlanId){

        flowRuleService.removeFlowRules(incomingInternetFlows.get(vlanId), outgoingInternetFlows.get(vlanId));
        incomingInternetFlows.remove(vlanId);
        outgoingInternetFlows.remove(vlanId);
        customersIP.remove(vlanId);

    }


    public void dnsService(int vlanId) {

        dnsEnabledCustomers.remove(vlanId);
        dnsEnabledCustomers.put(vlanId, true);

    }

    private void dnsCapture(){


        TrafficSelector.Builder dnsSelector = DefaultTrafficSelector.builder();
        dnsSelector.matchInPort(oltPort);
        dnsSelector.matchVlanId(outerTag);
        dnsSelector.matchEthType(Ethernet.TYPE_IPV4);
        dnsSelector.matchIPProtocol(IPv4.PROTOCOL_UDP);
        dnsSelector.matchUdpDst(TpPort.tpPort(53));

        FlowRule.Builder dnsRule = DefaultFlowRule.builder();
        dnsRule.withSelector(dnsSelector.build());
        dnsRule.withTreatment(DefaultTrafficTreatment.builder().punt().build());
        dnsRule.withPriority(49200);
        dnsRule.makePermanent();
        dnsRule.fromApp(appId);
        dnsRule.forTable(ACL_TABLE);
        dnsRule.forDevice(torId);

        flowRuleService.applyFlowRules(dnsRule.build());
    }


    public void noDnsService(int vlanId) {

        dnsEnabledCustomers.remove(vlanId);
        dnsEnabledCustomers.put(vlanId, false);

    }

    public void detectCustomerIP(int vlanId, Ip4Prefix dstIp){
        detectCustomerIP(outerTag.toShort(), vlanId, dstIp);
    }

    public void detectCustomerIP (int sockId, int vlanId, Ip4Prefix dstIp){
        //create a packet with proper tags

        byte[] bytesData = new byte[IP_LEARNING_LENGTH];
        bytesData[IP_LEARNING_LENGTH-4] = (byte) (sockId/256);
        bytesData[IP_LEARNING_LENGTH-3] = (byte) (sockId % 256);
        bytesData[IP_LEARNING_LENGTH-2] = (byte) (vlanId/256);
        bytesData[IP_LEARNING_LENGTH-1] = (byte) (vlanId % 256);
        Data data = new Data(bytesData);

        UDP udpPacket = new UDP();

        udpPacket.setPayload(data);
        data.setParent(udpPacket);

        udpPacket.setSourcePort(ipLearningPort.toInt());
        udpPacket.setDestinationPort(ipLearningPort.toInt());
        //responseUdp.resetChecksum();

        IPv4 ipPacket = new IPv4();

        ipPacket.setPayload(udpPacket);
        udpPacket.setParent(ipPacket);

        ipPacket.setSourceAddress(ipLearningOrigin.address().toInt());
        ipPacket.setDestinationAddress(dstIp.address().toInt());
        ipPacket.setIdentification(DEFAULT_ID);
        ipPacket.setProtocol(IPv4.PROTOCOL_UDP);
        ipPacket.setFlags(NO_FRAGMENT);
        ipPacket.setTtl(DEFAULT_TTL);
        //responseIp.resetChecksum();

        Ethernet ethernetPacket = new Ethernet();

        ethernetPacket.setPayload(ipPacket);
        ipPacket.setParent(ethernetPacket);

        ethernetPacket.setDestinationMACAddress(VSG_MAC);
        ethernetPacket.setSourceMACAddress(DEFAULT_SOURCE_MAC);
        ethernetPacket.setEtherType(Ethernet.TYPE_IPV4);

        ethernetPacket.setVlanID(Ethernet.TYPE_VLAN , (short) sockId, Ethernet.TYPE_VLAN, (short) vlanId);
        ethernetPacket.resetChecksum();

        TrafficTreatment.Builder ipLearningTreatment = DefaultTrafficTreatment.builder();
        ipLearningTreatment.setOutput(serverPort);

        ByteBuffer bb = ByteBuffer.wrap(ethernetPacket.serialize());

        OutboundPacket outboundPacket = new DefaultOutboundPacket(torId, ipLearningTreatment.build(), bb);
        packetService.emit(outboundPacket);

        log.info("Packet sent " + javax.xml.bind.DatatypeConverter.printHexBinary(bb.array()));


        //send it to the vSGs, it will be intercepted from the other side
    }

    public static void foundCustomerIP(int vlanId, Ip4Address ip){
        customersIP.remove(vlanId);
        customersIP.put(vlanId, ip);
        log.info("Custumer's IP found, vlanId : " + vlanId + " IP : " + ip);
    }
    public static void foundUverseCustomerIP(int vlanId, Ip4Address ip){
        customersUverseIP.remove(vlanId);
        customersUverseIP.put(vlanId, ip);
        log.info("Custumer's Uverse IP found, vlanId : " + vlanId + " IP : " + ip);
    }

    public void listen() {


        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket(JSON_COMMUNICATION_PORT);
                    while (true) {
                        Socket socket = serverSocket.accept();
                        readJson(socket);
                    }
                }catch(IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        };

        Thread listenningThread = new Thread(r);
        listenningThread.setDaemon(true);
        listenningThread.start();
        log.info("Thread listenning for the web interface");

    }

    private void readJson(Socket socket){


        Runnable r = new Runnable() {
            @Override
            public void run() {
                try{
                    log.info("Connection received");
                    InputStream is = socket.getInputStream();
                    OutputStream os = socket.getOutputStream();
                    JsonObject msg = (JsonObject) TorRest.extractJson(is);
                    if(treatJson(msg)){
                        os.write(HTTP_OK);
                        os.flush();
                    }
                    is.close();
                    os.close();
                    socket.close();
                }catch(Exception e){
                    log.error("treatment of JSON exception", e);
                    return;
                }
            }
        };

        Thread thread = new Thread(r);
        thread.setDaemon(true);
        thread.start();

    }

    boolean treatJson(JsonObject jsonMessage){

            String action = jsonMessage.getString("action").replaceAll("\\p{P}","");
            int vlanId = 0;
            int sockId = 0;
            try{
                vlanId = jsonMessage.getJsonNumber("C_Tag").intValue();
                //To be used when we have multiple OLTs
                sockId = jsonMessage.getJsonNumber("S_Tag").intValue();
            } catch (Exception e){
                log.warn("vlanId/sockId undefined", e);
            }

            log.info("action : " + action);

            if(action.equals(BRG_DETECTION)){
                try{
                    String mac = jsonMessage.getString("brgMac").replaceAll("\"", "");
                    this.newBrgDetectionCapture(MacAddress.valueOf(mac));
                    log.info("brg  Detection : " + mac);
                    return true;
                }catch (Exception e){
                    log.error("brgMac problem ", e);
                    return false;

                }

            }else {
                if (vlanId == 0) {
                    return false;
                }

                switch (action) {
                    case CONNECT:
                        this.connectInternet(vlanId);
                        break;
                    case DISCONNECT:
                        this.disconnectInternet(vlanId);
                        break;
                    case DNS_SERVICE:
                        this.dnsService(vlanId);
                        break;
                    case NO_DNS_SERVICE:
                        this.noDnsService(vlanId);
                        break;
                    case CONNECT_TV:
                        //this.connectUverse(vlanId);
                        break;
                    case DISCONNECT_TV:
                        //this.disconnectUverse(vlanId);
                        break;
                    default:
                        log.error("This message does not match any control message");
                        return false;

                }
                return  true;

            }



    }





}


