package org.onosproject.vcpe;

import org.apache.felix.scr.annotations.*;
import org.onlab.packet.*;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.*;
import org.onosproject.net.packet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.onosproject.vcpe.VcpeComponent;

import javax.json.*;

/**
 * Created by nick on 6/26/15.
 */
@Component(immediate = true)

public class LanSwitch {


    static final Logger log = LoggerFactory.getLogger(LanSwitch.class);

    private static FlowRuleService flowRuleService;
    private static PacketService packetService;
    private static ApplicationId appId;

    private static  DeviceId lanSwitch;
    private static PortNumber customerSidePort =  PortNumber.portNumber(1);


    private static HashMap<Integer, FlowRule> downstreamFlows = new HashMap<>();
    private static HashMap<Integer, FlowRule> igmpFlows = new HashMap<>();
    private static HashMap<Integer, FlowRule> dnsToControllerFlows = new HashMap<>();
    private static List<Integer> enabledCustomers = new LinkedList<>();

    private static HashMap<Integer, Integer> dnsService = new HashMap<>();
    private static HashMap<Integer, ParentalControlSettings> parentalControlSettings= new HashMap<>();

    private static LanDiscovery lanDiscovery;

    private static final int GENERAL_DNS = 1;
    private static final int PERSONAL_DNS = 2;
    private static final int NO_DNS = 3;


    private static Ip4Address multicastNat = Ip4Address.valueOf("10.225.9.16");

    public LanSwitch(){}

    LanSwitch(ApplicationId appId, FlowRuleService flowRuleService, PacketService packetService, DeviceId deviceId){
        this.appId = appId;
        this.flowRuleService = flowRuleService;
        this.packetService = packetService;
        lanSwitch = deviceId;
    }

    void initiate() {

        lanDiscovery = new LanDiscovery();
        log.info("LanSwitch initiated");
    }


    void initiateCustomer(int clientId){
        //Flow from the customers to the VCPEs
        TrafficSelector.Builder customerToVcpeSelector = DefaultTrafficSelector.builder();
        customerToVcpeSelector.matchVlanId(getVlanIdFromClientId(clientId));
        customerToVcpeSelector.matchInPort(customerSidePort);

        TrafficTreatment.Builder customerToVcpeTreatment = DefaultTrafficTreatment.builder();
        //customerToVcpeTreatment.popVlan();
        customerToVcpeTreatment.setOutput(getToVpcePortNumberFromClientId(clientId));

        FlowRule.Builder customerToVcpeFlow = DefaultFlowRule.builder();
        customerToVcpeFlow.fromApp(appId);
        customerToVcpeFlow.forDevice(lanSwitch);
        customerToVcpeFlow.makePermanent();
        customerToVcpeFlow.withPriority(41001);
        customerToVcpeFlow.withSelector(customerToVcpeSelector.build());
        customerToVcpeFlow.withTreatment(customerToVcpeTreatment.build());

        //Flow from the VCPEs to the customers
        TrafficSelector.Builder vcpeToCustomerSelector = DefaultTrafficSelector.builder();
        vcpeToCustomerSelector.matchInPort(getToVpcePortNumberFromClientId(clientId));

        TrafficTreatment.Builder vcpeToCustomerTreatment = DefaultTrafficTreatment.builder();
        vcpeToCustomerTreatment.pushVlan();
        vcpeToCustomerTreatment.setVlanId(getVlanIdFromClientId(clientId));
        vcpeToCustomerTreatment.setOutput(customerSidePort);

        FlowRule.Builder vcpeToCustomerFlow = DefaultFlowRule.builder();
        vcpeToCustomerFlow.fromApp(appId);
        vcpeToCustomerFlow.forDevice(lanSwitch);
        vcpeToCustomerFlow.makePermanent();
        vcpeToCustomerFlow.withPriority(41002);
        vcpeToCustomerFlow.withSelector(vcpeToCustomerSelector.build());
        vcpeToCustomerFlow.withTreatment(vcpeToCustomerTreatment.build());


        flowRuleService.applyFlowRules(customerToVcpeFlow.build(), vcpeToCustomerFlow.build());

        dnsService.put(clientId, NO_DNS);

        // Flows to drop dhcp
        TrafficSelector.Builder selector1 = DefaultTrafficSelector.builder();
        selector1.matchUdpDst(TpPort.tpPort(67));
        //necessary to enforce UDP dst
        selector1.matchIPProtocol((byte) 17);
        selector1.matchEthType(Ethernet.TYPE_IPV4);
        selector1.matchInPort(getToVhoPortNumberFromClientId(clientId));


        TrafficSelector.Builder selector2 = DefaultTrafficSelector.builder();
        selector2.matchUdpDst(TpPort.tpPort(68));
        // necessary to enforce UDP dst
        selector2.matchIPProtocol((byte) 17);
        selector2.matchEthType(Ethernet.TYPE_IPV4);
        selector2.matchInPort(getToVhoPortNumberFromClientId(clientId));

        TrafficTreatment.Builder drop = DefaultTrafficTreatment.builder();
        drop.drop();

        FlowRule.Builder dhcpDropFlow1 = DefaultFlowRule.builder();
        dhcpDropFlow1.fromApp(appId);
        dhcpDropFlow1.makePermanent();
        dhcpDropFlow1.forDevice(lanSwitch);
        dhcpDropFlow1.withPriority(50000);
        dhcpDropFlow1.withSelector(selector1.build());
        dhcpDropFlow1.withTreatment(drop.build());

        FlowRule.Builder dhcpDropFlow2 = DefaultFlowRule.builder();
        dhcpDropFlow2.fromApp(appId);
        dhcpDropFlow2.makePermanent();
        dhcpDropFlow2.forDevice(lanSwitch);
        dhcpDropFlow2.withPriority(50000);
        dhcpDropFlow2.withSelector(selector2.build());
        dhcpDropFlow2.withTreatment(drop.build());


        flowRuleService.applyFlowRules(dhcpDropFlow1.build(), dhcpDropFlow2.build());
    }

    HashMap<Integer,ParentalControlSettings> getParentalControlSettings(){
        return parentalControlSettings;
    }

    boolean hasPersonalDnsService(int clientId) {
        int service  = dnsService.get(clientId);
        if (service == PERSONAL_DNS) {
            return true;
        }else{
            return false;
        }

    }


    private static PortNumber getToVpcePortNumberFromClientId(int clientId){
        return PortNumber.portNumber(2*clientId);
    }

    private static PortNumber getToVhoPortNumberFromClientId(int clientId){
        return PortNumber.portNumber(2*clientId+1);
    }

    static int getClientIdFromVlanId(VlanId vlanId){
        return (vlanId.toShort());
    }
    static int getClientIdFromVlanId(short vlanId){
        return vlanId;
    }

    static VlanId getVlanIdFromClientId(int clientId){
        return  VlanId.vlanId((short) clientId);
    }

    void connectCustomer(int clientId){
        // creating the two flows
        log.info("connect customer to Uverse function");
        // check if the customer is already connected
        if (enabledCustomers.contains(clientId)) {
            log.info("customer already connected");
            return;
        }
        //The customer is not connected


        // Creation of the downstream flow, push the Vlan tag and send downstream

        TrafficSelector.Builder downstreamSelector = DefaultTrafficSelector.builder();
        downstreamSelector.matchInPort(getToVhoPortNumberFromClientId(clientId));

        TrafficTreatment.Builder downstreamTreatment = DefaultTrafficTreatment.builder();
        downstreamTreatment.pushVlan();
        downstreamTreatment.setVlanId(getVlanIdFromClientId(clientId));
        downstreamTreatment.setOutput(customerSidePort);

        FlowRule.Builder downstreamFlowBuilder = DefaultFlowRule.builder();
        downstreamFlowBuilder.fromApp(appId);
        downstreamFlowBuilder.withSelector(downstreamSelector.build());
        downstreamFlowBuilder.forDevice(lanSwitch);
        downstreamFlowBuilder.withPriority(42002);
        downstreamFlowBuilder.makePermanent();
        downstreamFlowBuilder.withTreatment(downstreamTreatment.build());

        FlowRule downstreamFlow = downstreamFlowBuilder.build();

        //register the flow and keep a reference to remove it later
        downstreamFlows.put(clientId, downstreamFlow);
        flowRuleService.applyFlowRules(downstreamFlow);

        //Nat the IGMP multicast

        TrafficSelector.Builder igmpSelector = DefaultTrafficSelector.builder();
        igmpSelector.matchVlanId(getVlanIdFromClientId(clientId));
        igmpSelector.matchIPProtocol((byte) 2);
        igmpSelector.matchInPort(customerSidePort);
        // necessary to enforce IP Protocol criteria
        igmpSelector.matchEthType(Ethernet.TYPE_IPV4);

        TrafficTreatment.Builder igmpTreatment = DefaultTrafficTreatment.builder();
        igmpTreatment.popVlan();
        igmpTreatment.setIpSrc(multicastNat);
        igmpTreatment.setOutput(getToVhoPortNumberFromClientId(clientId));

        FlowRule.Builder igmpRule = DefaultFlowRule.builder();
        igmpRule.fromApp(appId);
        igmpRule.makePermanent();
        igmpRule.forDevice(lanSwitch);
        igmpRule.withPriority(42001);
        igmpRule.withTreatment(igmpTreatment.build());
        igmpRule.withSelector(igmpSelector.build());

        FlowRule igmpFlow = igmpRule.build();

        igmpFlows.put(clientId, igmpFlow);
        flowRuleService.applyFlowRules(igmpFlow);

        enabledCustomers.add(clientId);
        log.info("client : " + clientId + " added to the enabledCustomer list");
    }

    void disconnectCustomer(int clientId) {

        log.info("disconnect customer function, client : " + clientId);

        if (!enabledCustomers.contains(clientId)) {
            log.info("this client is not connected");
            return;
        } else {
            log.info("ready to remove flows");
            //checking if the flows exist to avoid a possible crash
            FlowRule downstreamFlow = downstreamFlows.get(clientId);
            FlowRule igmpFlow = igmpFlows.get(clientId);

            downstreamFlows.remove(clientId);
            igmpFlows.remove(clientId);

            if(downstreamFlow != null){
                flowRuleService.removeFlowRules(downstreamFlow);
            }
            if(igmpFlow != null){
                flowRuleService.removeFlowRules(igmpFlow);
            }
            enabledCustomers.remove(new Integer(clientId));
            log.info("customer disconnected, flows removed");

        }
    }

    private void createDnsFlowToController(int clientId){
        if(dnsToControllerFlows.get(clientId) == null){
            //intercept the flows
            TrafficSelector.Builder dnsSelector = DefaultTrafficSelector.builder();
            dnsSelector.matchEthType(Ethernet.TYPE_IPV4);
            dnsSelector.matchIPProtocol((byte) 17);
            dnsSelector.matchUdpDst(TpPort.tpPort(53));
            dnsSelector.matchVlanId(getVlanIdFromClientId(clientId));

            FlowRule.Builder dnsRule = DefaultFlowRule.builder();
            dnsRule.fromApp(appId);
            dnsRule.forDevice(lanSwitch);
            dnsRule.makePermanent();
            dnsRule.withPriority(43000);
            dnsRule.withSelector(dnsSelector.build());
            dnsRule.withTreatment(DefaultTrafficTreatment.builder().punt().build());

            FlowRule dnsFlow = dnsRule.build();

            dnsToControllerFlows.put(clientId, dnsFlow);
            flowRuleService.applyFlowRules(dnsFlow);
        }
    }

    void generalDns(int clientId){
        log.info("generalDns function called");
        if(dnsService.get(clientId) == GENERAL_DNS){
            log.info("the customer is already registered for this service");
            return;
        }
        createDnsFlowToController(clientId);
        dnsService.remove(clientId);
        dnsService.put(clientId, GENERAL_DNS);
    }
    void personalDns(int clientId){
        log.info("personalDns function called");
        if(dnsService.get(clientId) == PERSONAL_DNS){
            log.info("the customer is already registered for this service");
            return;
        }
        createDnsFlowToController(clientId);
        dnsService.remove(clientId);
        dnsService.put(clientId, PERSONAL_DNS);
        if(parentalControlSettings.get(clientId) == null){
            parentalControlSettings.put(clientId, new ParentalControlSettings());
        }
        if(clientId == 7){
            TestParentalControl.fakeEntry(this, clientId);
        }
    }

    void noDns(int clientId){

        if(dnsService.get(clientId) == NO_DNS){
            log.info("the customer is already registered for this service");
            return;
        }
        FlowRule dnsFlow = dnsToControllerFlows.get(clientId);
        if(dnsFlow != null){
            dnsToControllerFlows.remove(clientId);
            flowRuleService.removeFlowRules(dnsFlow);
        }
        dnsService.remove(clientId);
        dnsService.put(clientId, NO_DNS);

    }

    void discoverLan(int clientId){
        lanDiscovery.discoverNetwork(clientId);
    }

    void processPacket(PacketContext context) {
        try {
            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();
            IPacket payload = ethPkt.getPayload();


            if (payload instanceof ARP) {
                return;
            }

            IPv4 ip4Packet = (IPv4) payload;

            int clientId = getClientIdFromVlanId(ethPkt.getVlanID());
            MacAddress deviceMac = ethPkt.getSourceMAC();


            IPacket layer4Packet = payload.getPayload();
            if (layer4Packet instanceof UDP) {
                processDnsPacket(context);
            } else if (layer4Packet instanceof ICMP) {
                processIcmpPacket(context);
            }

        }catch (Exception e){
            log.error("packet processing exception", e);
        }
    }


    private void processDnsPacket(PacketContext context) {
        try {
            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();
            IPacket payload = ethPkt.getPayload();



            IPv4 ip4Packet = (IPv4) payload;

            int clientId = getClientIdFromVlanId(ethPkt.getVlanID());
            MacAddress deviceMac = ethPkt.getSourceMAC();


            IPacket udpPacket = payload.getPayload();
            UDP udpHeaderPacket = null;

            try {
                udpHeaderPacket = (UDP) udpPacket;
            } catch (Exception e) {
                log.error("udp casting error", e);
            }
            byte[] request = udpPacket.getPayload().serialize();

            //debug
            StringBuilder logMessage = new StringBuilder();
            logMessage.append("dns request payload received : ");
            for (int i = 0; i < request.length; i++) {
                logMessage.append(request[i]);
                logMessage.append(' ');
            }
            log.info(logMessage.toString());

            String url = "exception";
            try {
                url = DnsProxy.extractDomainNameFromRequest(request);
            } catch (Exception e) {
                log.error("exception dns resolution : ", e);
            }
            log.info(" DNS request : " + url);

            if (dnsService.get(clientId) == PERSONAL_DNS) {
                log.info("personal dns");
                ParentalControlSettings settings = parentalControlSettings.get(clientId);
                //Check if the device is restricted
                if (settings.isDeviceRestricted(deviceMac)) {
                    log.info("device is restricted");
                    //if so, check if the request match a blacklist category
                    Hashtable blacklist = ParseStructure.getTable();
                    Vector<String> categories = (Vector<String>) blacklist.get(url);
                    log.info("blacklist contains : " + blacklist.values().size() + " elements");
                    if (categories != null) {
                        log.info("this url belongs to  a restricted category. Checking if this category is allowed for this device");
                        if (!settings.isAllowed(deviceMac, categories)) {
                            //We should block this
                            log.info("DNS blocked : " + url);
                            return;
                        }
                    }
                }
            }
            //Unrestricted DNS
            ExecutorService ex = Executors.newSingleThreadExecutor();
            Future<byte[]> future = ex.submit(DnsProxy.newResolution(request));
            byte[] response;
            try {
                response = future.get(5000, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                log.error("future exception", e);
                return;
            }
            log.info("DNS response retrieved");

            TrafficTreatment.Builder dnsIpSpoofing = DefaultTrafficTreatment.builder();
            dnsIpSpoofing.setOutput(customerSidePort);

            //TODO : verify that the header are present
            //Recomposition of a new packet
            Data responseData = new Data(response);

            UDP responseUdp = new UDP();

            responseUdp.setPayload(responseData);
            responseData.setParent(responseUdp);

            responseUdp.setSourcePort(udpHeaderPacket.getDestinationPort());
            responseUdp.setDestinationPort(udpHeaderPacket.getSourcePort());
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
            responseEthernet.setVlanID(ethPkt.getVlanID());
            //responseEthernet.resetChecksum();

            StringBuffer sizeLog = new StringBuffer();
            sizeLog.append("Data size : ");
            sizeLog.append(responseData.serialize().length);
            sizeLog.append(", UDP size : ");
            sizeLog.append(responseUdp.serialize().length);
            sizeLog.append(", IP size : ");
            sizeLog.append(responseIp.serialize().length);
            sizeLog.append(", Ethernet size : ");
            sizeLog.append(responseEthernet.serialize().length);

            log.info(sizeLog.toString());

            OutboundPacket outboundPacket = new DefaultOutboundPacket(lanSwitch, dnsIpSpoofing.build(), ByteBuffer.wrap(responseEthernet.serialize()));

            packetService.emit(outboundPacket);
            log.info("DNS response created and sent");
            //debug
            StringBuffer buffer = new StringBuffer("DNS message sent, ");
            byte[] dnsResponseBytes  = responseEthernet.serialize();
            buffer.append(dnsResponseBytes.length);
            buffer.append((" bytes : "));
            for(int i =0; i< dnsResponseBytes.length; i++){
                buffer.append(dnsResponseBytes[i]);
                buffer.append(' ');
            }

            log.info(buffer.toString());
        } catch (Exception e) {
            log.error("exception during DNS", e);
        }

    }

    private void processIcmpPacket(PacketContext context){

        InboundPacket pkt = context.inPacket();
        Ethernet ethPkt = pkt.parsed();
        IPacket payload = ethPkt.getPayload();

        IPv4 ip4Packet = (IPv4) payload;

        int clientId = getClientIdFromVlanId(ethPkt.getVlanID());
        MacAddress deviceMac = ethPkt.getSourceMAC();
        Ip4Address deviceIp = Ip4Address.valueOf(ip4Packet.getSourceAddress());

        lanDiscovery.newDeviceFound(clientId,new DeviceFound(deviceMac, deviceIp));
    }

    class DeviceFound{

        private final String MAC = "macAddr";
        private final String IP = "ipAddr";
        private final String VENDOR = "vendor";

        MacAddress mac;
        Ip4Address ip;

        DeviceFound(MacAddress deviceMac, Ip4Address deviceIp){
            mac = deviceMac;
            ip = deviceIp;
        }

        public JsonObject createJson(String vendor){
            JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
            jsonBuilder.add(MAC, mac.toString());
            jsonBuilder.add(IP, ip.toString());
            jsonBuilder.add(VENDOR, vendor);
            //TODO : remove
            jsonBuilder.add("deviceGroup", "default");
            jsonBuilder.add("clientId", "default");
            jsonBuilder.add("deviceName", "default");
            jsonBuilder.add("assignedTo", "default");

            return jsonBuilder.build();

        }

        public String getMAC(){
            return mac.toString().replace(':', '-');
        }


    }


    class LanDiscovery {

        private static  final int DEFAULT_MASK_LENGTH = 24;
        private final int DEFAULT_SUBNET = IPv4.toIPv4Address("192.168.1.0");//IpAddress.valueOf("192.168.1.0")
        private final int DEFAULT_ONOS_ADDRESS = IPv4.toIPv4Address("192.168.1.77");

        private final String VENDOR_LOOKUP_URL = "http://www.macvendorlookup.com/api/v2/";

        private final  String UNKNOWN_VENDOR = "Unknown";
        private final String VENDOR = "company";
        private final String CONTROL_WEBSERVER_URL = "http://prodmos.foundry.att.com/d2/sdn/vrg/nick/clienttest";
        private final String CHARSET = StandardCharsets.UTF_8.name();

        private final  int NO_CONTENT = 204;
        private final byte NO_FRAGMENT = (byte) 2;
        private final short DEFAULT_ID = (short) 1111;
        private final byte DEFAULT_TTL = 64;

        private HashMap<Integer,List<DeviceFound>> devices = new HashMap<>();

        public void discoverNetwork( int clientId){
            discoverNetwork(clientId, DEFAULT_MASK_LENGTH);
        }

        public LanDiscovery(){}

        /*
        * Create and send a ping to each IP address in the LAN
        * Create a flow to catch the response
        * Lookup the vendor from the mac Address
        * Create a Json with the information of all discovered devices and send it  to   the webserver
        *
        * */
        public void discoverNetwork( int clientId, int maskLength){

            Runnable r =  new Runnable() {
                @Override
                public void run() {

                    //Intercept the ICMP traffic, ping response
                    TrafficSelector.Builder pingResponseSelector = DefaultTrafficSelector.builder();
                    pingResponseSelector.matchVlanId(getVlanIdFromClientId(clientId));
                    pingResponseSelector.matchInPort(customerSidePort);
                    pingResponseSelector.matchEthType(Ethernet.TYPE_IPV4);
                    pingResponseSelector.matchIPProtocol(IPv4.PROTOCOL_ICMP);
                    pingResponseSelector.matchIcmpType(ICMP.TYPE_ECHO_REPLY);

                    FlowRule.Builder pingResponseRule = DefaultFlowRule.builder();
                    pingResponseRule.fromApp(appId);
                    pingResponseRule.forDevice(lanSwitch);
                    pingResponseRule.makePermanent();
                    pingResponseRule.withPriority(49000);
                    pingResponseRule.withSelector(pingResponseSelector.build());
                    pingResponseRule.withTreatment(DefaultTrafficTreatment.builder().punt().build());

                    FlowRule pingResponseFlow = pingResponseRule.build();

                    flowRuleService.applyFlowRules(pingResponseFlow);

                    log.info("interception flows installed");

                    devices.put(clientId, new LinkedList<>());

                    // To  make sure that the flow is setup when the first response come back
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                        log.error("Exception during sleep", e);
                    }

                    log.info("creation of the ICMP packet");

                    //Create the ICMP packet
                    byte[] bytesData = new byte[4];
                    bytesData[0]= 57;
                    bytesData[1] = 32;
                    bytesData[3] =1;

                    Data data = new Data(bytesData);

                    ICMP ping = new ICMP();

                    ping.setPayload(data);
                    data.setParent(ping);

                    ping.setIcmpType(ICMP.TYPE_ECHO_REQUEST);

                    IPv4 ipPing = new IPv4();

                    ipPing.setPayload(ping);
                    ping.setParent(ipPing);

                    ipPing.setDestinationAddress(DEFAULT_SUBNET);
                    ipPing.setSourceAddress(DEFAULT_ONOS_ADDRESS);
                    ipPing.setProtocol(IPv4.PROTOCOL_ICMP);
                    ipPing.setFlags(NO_FRAGMENT);
                    ipPing.setIdentification(DEFAULT_ID);
                    ipPing.setTtl(DEFAULT_TTL);


                    Ethernet EthernetPing = new Ethernet();

                    EthernetPing.setPayload(ipPing);
                    ipPing.setParent(EthernetPing);

                    log.info("Ethernet packet associated, not completed");

                    EthernetPing.setDestinationMACAddress(MacAddress.BROADCAST);
                    EthernetPing.setSourceMACAddress(MacAddress.valueOf(DEFAULT_ONOS_ADDRESS));
                    EthernetPing.setEtherType(Ethernet.TYPE_VLAN);
                    EthernetPing.setVlanID((short) clientId);



                    StringBuilder buffer = new StringBuilder();
                    buffer.append("ICMP packet size : ");
                    buffer.append(ping.serialize().length);
                    buffer.append(", IP packet size : ");
                    buffer.append(ipPing.serialize().length);
                    buffer.append(", Ethernet packet size : ");
                    buffer.append(EthernetPing.serialize().length);
                    buffer.append("Ethernet packet : ");
                    byte[] serialized = EthernetPing.serialize();
                    for(int i = 0; i< serialized.length; i++){
                        buffer.append(serialized[i]);
                    }
                    log.info(buffer.toString());

                    TrafficTreatment.Builder lanDiscoveryTreatment = DefaultTrafficTreatment.builder();
                    lanDiscoveryTreatment.setOutput(customerSidePort);

                    log.info("ICMP packet ready to send ");
                    int j = 0;
                    for (int i = 1; i < (int) (Math.pow(2, 32 - maskLength))-2; i++) {
                        ipPing.setDestinationAddress(DEFAULT_SUBNET + i);
                        data.resetChecksum();
                        OutboundPacket outboundPacket = new DefaultOutboundPacket(lanSwitch, lanDiscoveryTreatment.build(), ByteBuffer.wrap(EthernetPing.serialize()));
                        packetService.emit(outboundPacket);
                        j++;
                    }
                    log.info ( j + " packets emitted");
                    // waiting for the responses
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        log.error("Exception during sleep", e);
                    }

                    flowRuleService.removeFlowRules(pingResponseFlow);
                    log.info("Wait time is over");
                    List<DeviceFound> customerDevices = getCustomerDevices(clientId);
                    log.info("5");
                    JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                    log.info(" customer devices data retrieved, looking up the vendor for " + customerDevices.size() + " devices");

                    //Look up the vendor
                    for(DeviceFound device : customerDevices){
                        try{
                            HttpURLConnection httpConnection = (HttpURLConnection) new URL(VENDOR_LOOKUP_URL + device.getMAC()).openConnection();
                            httpConnection.setRequestMethod("GET");
                            int  status = httpConnection.getResponseCode();
                            if(status == NO_CONTENT){
                                //vendor  not known
                                arrayBuilder.add(device.createJson(UNKNOWN_VENDOR));
                                log.info("Unknown vendor");
                            }else{
                                InputStream is = httpConnection.getInputStream();
                                JsonReader reader = Json.createReader(is);
                                JsonArray jsonArrayResponse = reader.readArray();
                                JsonObject jsonResponse = jsonArrayResponse.getJsonObject(0);
                                arrayBuilder.add(device.createJson(jsonResponse.getString(VENDOR)));
                                reader.close();
                                is.close();
                            }

                        }catch (Exception e){
                            log.error ("Vendor lookup exception", e);
                        }
                    }
                    JsonArray devicesData = arrayBuilder.build();

                    log.info(devicesData.toString());

                    JsonObjectBuilder customerBuilder = Json.createObjectBuilder();
                    customerBuilder.add("firstName", "Nick");
                    customerBuilder.add("lastName", "Palpacuer");
                    customerBuilder.add("description", "");
                    customerBuilder.add("city", "Atlanta");
                    customerBuilder.add("state", "Georgia");
                    customerBuilder.add("zip", "");
                    customerBuilder.add("address1", "");
                    customerBuilder.add("address2", "");
                    customerBuilder.add("custBan", "nicksBan");


                    JsonObjectBuilder responseBuilder = Json.createObjectBuilder();
                    responseBuilder.add("customer", customerBuilder);
                    responseBuilder.add("devices", arrayBuilder);

                    JsonObject jsonToSend = responseBuilder.build();

                    //Send the Json
                    try{
                        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(CONTROL_WEBSERVER_URL + clientId).openConnection();
                        httpURLConnection.setRequestMethod("POST");
                        httpURLConnection.setDoOutput(true);
                        httpURLConnection.setRequestProperty("Content-Type", "application/json");
                        httpURLConnection.setRequestProperty("Accept-Charset", CHARSET);
                        OutputStream out = httpURLConnection.getOutputStream();
                        JsonWriter jsonOut = Json.createWriter(out);
                        jsonOut.write(jsonToSend);
                        jsonOut.close();
                        out.close();
                    }catch (Exception e){
                        log.error("Exception sending the devices  data", e);
                    }
                }

            };

            Thread lanDiscoveryThread = new Thread(r);
            lanDiscoveryThread.start();
            log.info("Lan discovery thread started");

        }

        public synchronized void newDeviceFound(int clientId, DeviceFound newDevice){
            devices.get(clientId).add(newDevice);
        }

        public synchronized List<DeviceFound> getCustomerDevices(int clientId){
            List<DeviceFound> customerDevices = devices.get(clientId);
            devices.remove(clientId);
            return customerDevices;
        }
    }



}


