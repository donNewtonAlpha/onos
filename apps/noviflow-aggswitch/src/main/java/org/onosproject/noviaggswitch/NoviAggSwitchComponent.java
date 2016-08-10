
package org.onosproject.noviaggswitch;


import org.apache.felix.scr.annotations.*;


import org.onlab.packet.*;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.driver.extensions.NoviflowMatchVni;
import org.onosproject.driver.extensions.NoviflowPopVxLan;
import org.onosproject.driver.extensions.NoviflowSetVxLan;
import org.onosproject.driver.extensions.ofmessages.OFNoviflowVniExperimenterMsg;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.*;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.*;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.meter.*;
import org.onosproject.net.packet.*;
import org.onosproject.noviaggswitch.config.NoviAggSwitchConfig;
import org.onosproject.noviaggswitch.config.NoviAggSwitchConfigListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Semaphore;


/**
 * Created by nick on 6/26/15.
 */
@Component(immediate = true)

public class NoviAggSwitchComponent {


    static final Logger log = LoggerFactory.getLogger(NoviAggSwitchComponent.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MeterService meterService;  
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;




    
    static ApplicationId appId;

    static final DeviceId deviceId = DeviceId.deviceId("of:000000223d5a00d9");



    private static MacAddress switchMac = MacAddress.valueOf("68:05:33:44:55:66");
    private static Ip4Address aggSwitchIP = Ip4Address.valueOf("10.50.1.1");
    private static Ip4Address primaryLinkIP = Ip4Address.valueOf("10.20.1.1");
    private static Ip4Address secondaryLinlkIP = Ip4Address.valueOf("10.20.2.1");

    private PortNumber bngPort;
    private PortNumber secondaryBngPort;

    private NoviAggSwitchPacketProcessor processor;
    private LinkFailureDetection linkFailureDetection;
    private NoviAggSwitchConfigListener cfgListener;


    private static NoviAggSwitchComponent instance = null;


    public static NoviAggSwitchComponent getComponent() {
        return instance;

    }



    @Activate
    protected void activate() {

        log.debug("trying to activate");
        instance = this;
        appId = coreService.registerApplication("org.onosproject.noviaggswitch");


        bngPort =  PortNumber.portNumber(7);
        secondaryBngPort = PortNumber.portNumber(8);
        /*        //Config
        cfgListener = new NoviAggSwitchConfigListener();
        cfgService.registerConfigFactory(cfgListener.getCfgAppFactory());
        cfgService.addListener(cfgListener);*/
        

        //Packet processor
        processor = new NoviAggSwitchPacketProcessor(packetService);
        packetService.addProcessor(processor, 1);
        
        //Routing info
        //4 info

        //loopback
        processor.addRoutingInfo(deviceId, bngPort, Ip4Prefix.valueOf(aggSwitchIP, 24), aggSwitchIP, MacAddress.valueOf("00:00:00:00:00:00"));
        processor.addRoutingInfo(deviceId, secondaryBngPort, Ip4Prefix.valueOf(aggSwitchIP, 24), aggSwitchIP, MacAddress.valueOf("00:00:00:00:00:00"));
        //Uplinks
        processor.addRoutingInfo(deviceId, PortNumber.portNumber(7), Ip4Prefix.valueOf(primaryLinkIP, 24), primaryLinkIP, MacAddress.valueOf("68:05:11:11:11:11"));
        processor.addRoutingInfo(deviceId, PortNumber.portNumber(8), Ip4Prefix.valueOf(secondaryLinlkIP, 24), secondaryLinlkIP, MacAddress.valueOf("68:05:22:22:22:22"));


        //LinkFailureDetection
        List<ConnectPoint> redundancyPorts = new LinkedList<>();
        redundancyPorts.add(new ConnectPoint(deviceId, PortNumber.portNumber(7)));
        redundancyPorts.add(new ConnectPoint(deviceId, PortNumber.portNumber(8)));

        linkFailureDetection = new LinkFailureDetection(flowRuleService, redundancyPorts);
        deviceService.addListener(linkFailureDetection);







        //IPs the agg switch is responding to ARP
        //arpIntercept(aggSwitchIP);
        arpIntercept(primaryLinkIP);
        arpIntercept(secondaryLinlkIP);

        //IPs the agg switch is responding to ping
        icmpIntercept(aggSwitchIP);
        icmpIntercept(primaryLinkIP);
        icmpIntercept(secondaryLinlkIP);





        log.info("NoviFlow AggSwitch activated");

    }


    @Deactivate
    protected void deactivate() {

        flowRuleService.removeFlowRulesById(appId);


        Iterable<Group> appGroups = groupService.getGroups(deviceId, appId);
        for(Group group : appGroups) {
            groupService.removeGroup(group.deviceId(), group.appCookie(), group.appId());
        }

        //Clear meters
        Collection<Meter> meters = meterService.getMeters(deviceId);
        for(Meter meter : meters) {
            if(meter.appId().equals(appId)){
                meterService.withdraw(DefaultMeterRequest.builder().remove(), meter.id());
            }
        }

        packetService.removeProcessor(processor);



        log.info("Stopped");
    }

    public void addAccessDevice(int port, int vni, String bngVxlanIp) {

        addAccessDevice(port, vni, bngVxlanIp, bngVxlanIp, bngVxlanIp);

    }

    public void addAccessDevice(int port, int vni, String bngVxlanIp, String viaPrimaryIP, String viaSecondaryIP) {

        Random rand = new Random();
        int udpPort = rand.nextInt() + 2000;


        addAccessDevice(port, vni, udpPort, bngVxlanIp, viaPrimaryIP, viaSecondaryIP, aggSwitchIP.toString(), switchMac.toString());

    }

    private void addAccessDevice(int port, int vni, int udpPort, String bngVxlanIp, String viaPrimaryIP, String viaSecondaryIP, String switchVxlanIp, String switchVxlanMac) {

        Runnable r = new Runnable() {

            @Override
            public void run() {
                Ip4Address bngVxLanIP = Ip4Address.valueOf(bngVxlanIp);

                //PrimaryPath
                MacAddress bngVxLanPrimaryMac = processor.getMac(Ip4Address.valueOf(viaPrimaryIP));
                if(bngVxLanPrimaryMac != null) {
                    log.info("MAC found, ready to add flows");

                    try {
                        accessToBng(PortNumber.portNumber(port), vni, udpPort, bngVxLanIP, bngVxLanPrimaryMac, Ip4Address.valueOf(switchVxlanIp), MacAddress.valueOf(switchVxlanMac), true);
                        bngToAccess(PortNumber.portNumber(port), vni, Ip4Address.valueOf(bngVxlanIp), true);

                    } catch (Exception e) {
                        log.warn("Exception", e);
                    }

                    //SecondaryPath if different from primary

                    if (!viaSecondaryIP.equals(viaPrimaryIP)) {

                        MacAddress bngVxLanSecondaryMac = processor.getMac(Ip4Address.valueOf(viaSecondaryIP));
                        if(bngVxLanSecondaryMac != null) {
                            log.info("MAC found, ready to add flows");

                            try {
                                accessToBng(PortNumber.portNumber(port), vni, udpPort, bngVxLanIP, bngVxLanSecondaryMac, Ip4Address.valueOf(switchVxlanIp), MacAddress.valueOf(switchVxlanMac), false);
                                bngToAccess(PortNumber.portNumber(port), vni, Ip4Address.valueOf(bngVxlanIp), false);

                            } catch (Exception e) {
                                log.warn("Exception", e);
                            }
                        }
                    }
                }
            }
        };

        Thread t = new Thread(r);
        t.setDaemon(true);
        t.start();

    }

/*    private void addAccessDevice(int port, int vni, int udpPort, String bngVxlanIp, String switchVxlanIp, String switchVxlanMac) {

        addAccessDevice(port, vni, udpPort, bngVxlanIp, bngVxlanIp, switchVxlanIp, switchVxlanMac);

    }*/

    private void accessToBng(PortNumber port, int vni, int udpPort, Ip4Address bngVxlanIp, MacAddress bngVxlanMac, Ip4Address switchVxlanIp, MacAddress switchVxlanMac, boolean primary){

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchInPort(port);


        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.extension(new NoviflowSetVxLan(switchVxlanMac,bngVxlanMac, switchVxlanIp, bngVxlanIp, udpPort, vni), deviceId);
        if(primary) {
            treatment.setOutput(bngPort);
        } else {
            treatment.setOutput(secondaryBngPort);
        }

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        if(primary) {
            rule.withPriority(2000);
        } else {
            rule.withPriority(1000);
        }
        rule.forTable(0);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        flowRuleService.applyFlowRules(rule.build());

    }


    private void bngToAccess(PortNumber port, int vni, Ip4Address bngVxLanIP, boolean primary) {

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        if(primary) {
            selector.matchInPort(bngPort);
        } else {
            selector.matchInPort(secondaryBngPort);
        }
        selector.matchIPSrc(bngVxLanIP.toIpPrefix());


        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.extension(new NoviflowPopVxLan(), deviceId);
        treatment.setOutput(port);

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        if(primary) {
            rule.withPriority(1500);
        } else {
            rule.withPriority(750);
        }
        rule.forTable(0);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        flowRuleService.applyFlowRules(rule.build());

    }



    private void arpIntercept(Ip4Address respondFor) {
        
        //TODO : make a table rule and integrate IGMP  (table 20) ?

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_ARP);
        selector.matchArpTpa(respondFor);


        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.punt();

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(15000);
        rule.forTable(0);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        flowRuleService.applyFlowRules(rule.build());
    }

    private void icmpIntercept(Ip4Address respondFor) {

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchIPDst(respondFor.toIpPrefix());
        selector.matchIPProtocol(IPv4.PROTOCOL_ICMP);


        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.punt();

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(10000);
        rule.forTable(0);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        flowRuleService.applyFlowRules(rule.build());

    }


    /*private void noviExpSetup() {

        Runnable r = new Runnable() {
            @Override
            public void run() {

                OFNoviflowVniExperimenterMsg msg = new OFNoviflowVniExperimenterMsg(0);

                try {
                    byte[] ip = new byte[4];
                    ip[0] = 10;
                    ip[1] = 64;
                    ip[2] = 1;
                    ip[3] = 85;
                    InetAddress dstIp = InetAddress.getByAddress(ip);
                    ip[3] = 50;
                    InetAddress sourceIp = InetAddress.getByAddress(ip);
                    log.info("Source : " + sourceIp);
                    log.info("Destination : " + dstIp);
                    Socket socket = new Socket(dstIp, 40095, sourceIp, 6653);
                    OutputStream os = socket.getOutputStream();

                    InputStream is = socket.getInputStream();

                    while (true) {
                        os.write(msg.fullIPPayload());
                        Thread.sleep(1000);

                        if (is.available() > 0) {
                            byte[] response = new byte[is.available()];
                            is.read(response);
                            //send to log

                            StringBuilder builder = new StringBuilder("Response : ");
                            for (int i = 0; i < response.length; i++) {
                                builder.append(response[i]);
                                builder.append(' ');
                            }
                            log.info(builder.toString());

                        }
                    }


                } catch (Exception e) {
                    log.error("Exception noviExpSetup", e);
                }
            }
        };

        Thread t =new Thread(r);
        t.setDaemon(true);
        t.start();

    }*/



    /*private class NoviBngPacketProcessor implements PacketProcessor {

        List<MacRequest> macRequests = new LinkedList<>();

        @Override
        public void process(PacketContext context) {
            if (context.isHandled()) {
                return;
            }

            try {
                InboundPacket pkt = context.inPacket();


                Ethernet ethPkt = pkt.parsed();
                IPacket payload = ethPkt.getPayload();

                if (payload instanceof ARP) {

                    log.debug("ARP packet received");

                    ARP arp = (ARP) payload;

                    PortNumber inPort = pkt.receivedFrom().port();

                    if (arp.getOpCode() == ARP.OP_REQUEST) {
                        log.info("It is an ARP request");
                        handleArpRequest(inPort, ethPkt);
                    } else {
                        log.debug("It is an ARP reply");
                        handleArpReply(inPort, ethPkt);
                    }
                } else if(payload instanceof IPv4) {

                    log.info("IP packet received");
                    IPv4 ipPkt = (IPv4) payload;

                    IPacket ipPayload = ipPkt.getPayload();

                    if(ipPayload instanceof ICMP) {

                        log.info("ICMP packet received");

                        ICMP ping = (ICMP) ipPayload;

                        PortNumber inPort = pkt.receivedFrom().port();

                        if (ping.getIcmpType() == ICMP.TYPE_ECHO_REQUEST) {
                            log.info("It is a ping request");
                            handlePingRequest(inPort, ethPkt);
                        } else {
                            if (ping.getIcmpType() == ICMP.TYPE_ECHO_REPLY)
                                log.debug("It is a ping reply");
                            //TODO
                        }
                    }
                }
            } catch(Exception e){
                log.error("Exception during processing" , e);
            }
        }
        

        private void handleArpRequest(PortNumber inPort, Ethernet ethPkt) {
            ARP arpRequest = (ARP) ethPkt.getPayload();
            

            // ARP request for router. Send ARP reply.
            if (isArpForTor(arpRequest)) {
                sendArpResponse(ethPkt, arpRequest, inPort);
            }
        }

        private void handleArpReply(PortNumber inPort, Ethernet ethPkt) {
            ARP arpReply = (ARP) ethPkt.getPayload();
            log.info("ARP reply");


            // ARP reply for router. Process all pending IP packets.
            Ip4Address hostIpAddress = Ip4Address.valueOf(arpReply.getSenderProtocolAddress());
            for (MacRequest request : macRequests) {
                if (request.getIp().equals(hostIpAddress)) {
                    MacAddress mac = MacAddress.valueOf(arpReply.getSenderHardwareAddress());

                    request.setMac(mac);
                    request.unlock();
                    log.info("requested MAC for " + request.getIp() +" found");
                    macRequests.remove(request);
                }
            }
        }

        private void handlePingRequest(PortNumber inPort, Ethernet ethPkt) {

            IPv4 ipPkt = (IPv4) ethPkt.getPayload();


            Ip4Address requestIpAddress = Ip4Address.valueOf(ipPkt.getDestinationAddress());
            if(requestIpAddress.equals(aggSwitchIP)) {

                log.info("This ping request is for this switch");
                sendICMPreply(inPort, ethPkt);

            }

        }

        private void sendICMPreply(PortNumber port, Ethernet ethPkt) {

            log.info("creation of the ICMP reply");

            IPv4 ipPkt = (IPv4) ethPkt.getPayload();
            ICMP ping = (ICMP) ipPkt.getPayload();

            ICMP pingResponse = new ICMP();
            pingResponse.setIcmpType(ICMP.TYPE_ECHO_REPLY);
            pingResponse.setIcmpCode(ICMP.SUBTYPE_ECHO_REPLY);
            pingResponse.setChecksum((short) 0);
            pingResponse.setPayload(ping.getPayload());

            IPv4 ipResponse = new IPv4();
            ipResponse.setDestinationAddress(ipPkt.getSourceAddress());
            ipResponse.setSourceAddress(ipPkt.getDestinationAddress());
            ipResponse.setTtl((byte) 64);
            ipResponse.setChecksum((short)0);

            ipResponse.setPayload(pingResponse);
            //pingResponse.setParent(ipResponse);


            //Modify the original request and send it back : It keeps the proper vlan tagging
            ethPkt.setDestinationMACAddress(ethPkt.getSourceMAC())
                    .setSourceMACAddress(switchMac)
                    .setEtherType(Ethernet.TYPE_IPV4)
                    .setPayload(ipResponse);


            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            treatment.setOutput(port);
            OutboundPacket outPacket = new DefaultOutboundPacket(deviceId,
                    treatment.build(), ByteBuffer.wrap(ethPkt.serialize()));

            packetService.emit(outPacket);
            log.info("ICMP reply response sent");

        }


        private boolean isArpForTor(ARP arpMsg) {
            Ip4Address targetProtocolAddress = Ip4Address.valueOf(
                    arpMsg.getTargetProtocolAddress());
            return (targetProtocolAddress.equals(aggSwitchIP));
        }


        public void sendArpRequest(IpAddress targetAddress, IpAddress sourceAddress, PortNumber dstPort, VlanId vlanId) {


            ARP arpRequest = new ARP();
            arpRequest.setHardwareType(ARP.HW_TYPE_ETHERNET)
                    .setProtocolType(ARP.PROTO_TYPE_IP)
                    .setHardwareAddressLength(
                            (byte) Ethernet.DATALAYER_ADDRESS_LENGTH)
                    .setProtocolAddressLength((byte) Ip4Address.BYTE_LENGTH)
                    .setOpCode(ARP.OP_REQUEST)
                    .setSenderHardwareAddress(switchMac.toBytes())
                    .setTargetHardwareAddress(MacAddress.ZERO.toBytes())
                    .setSenderProtocolAddress(sourceAddress.toOctets())
                    .setTargetProtocolAddress(targetAddress.toOctets());

            Ethernet eth = new Ethernet();
            eth.setDestinationMACAddress(MacAddress.BROADCAST.toBytes())
                    .setSourceMACAddress(switchMac)
                    .setEtherType(Ethernet.TYPE_ARP).setPayload(arpRequest);

            if(!vlanId.equals(VlanId.NONE)){
                eth.setVlanID(vlanId.toShort());
            }

            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            treatment.setOutput(dstPort);
            OutboundPacket outPacket = new DefaultOutboundPacket(deviceId,
                    treatment.build(), ByteBuffer.wrap(eth.serialize()));

            packetService.emit(outPacket);
        }

        private void sendArpResponse(Ethernet eth, ARP arpRequest, PortNumber dstPort) {

            log.info("Preparing to send ARP response !!");

            ARP arpReply = new ARP();
            arpReply.setHardwareType(ARP.HW_TYPE_ETHERNET)
                    .setProtocolType(ARP.PROTO_TYPE_IP)
                    .setHardwareAddressLength(
                            (byte) Ethernet.DATALAYER_ADDRESS_LENGTH)
                    .setProtocolAddressLength((byte) Ip4Address.BYTE_LENGTH)
                    .setOpCode(ARP.OP_REPLY)
                    .setSenderHardwareAddress(switchMac.toBytes())
                    .setSenderProtocolAddress(arpRequest.getTargetProtocolAddress())
                    .setTargetHardwareAddress(arpRequest.getSenderHardwareAddress())
                    .setTargetProtocolAddress(arpRequest.getSenderProtocolAddress());


            //Modify the original request and send it back : It keeps the proper vlan tagging
            eth.setDestinationMACAddress(arpRequest.getSenderHardwareAddress())
                    .setSourceMACAddress(switchMac)
                    .setEtherType(Ethernet.TYPE_ARP)
                    .setPayload(arpReply);

            //log.info("ARP integrated to Ethernet packet, ready to send");

            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            treatment.setOutput(dstPort);
            OutboundPacket outPacket = new DefaultOutboundPacket(deviceId,
                    treatment.build(), ByteBuffer.wrap(eth.serialize()));

            packetService.emit(outPacket);
            log.info("ARP response sent");
        }

        public MacAddress getMac(Ip4Address ip) {

            MacRequest request = new MacRequest(ip);
            macRequests.add(request);
            sendArpRequest(ip, primaryLinkIP, bngPort, VlanId.NONE);
            sendArpRequest(ip, secondaryLinlkIP, secondaryBngPort, VlanId.NONE);
            log.info("ARP request for : " + ip.toString());
            request.lock();

            log.info("MAC found : " + request.getMac() + " for " + ip);
            return request.getMac();


        }

        private class MacRequest {

            private Ip4Address ip;
            private Semaphore lock;
            private MacAddress matchingMac;

            MacRequest(Ip4Address ip) {
                this.ip = ip;
                lock = new Semaphore(-1);
            }

            public void lock() {
                try{
                    lock.acquire();
                } catch (Exception e) {
                    log.error("Lock exception : " , e);
                }
            }

            public void unlock(){
                lock.release(2);
            }

            public void setMac(MacAddress mac) {
                matchingMac = mac;
            }

            public MacAddress getMac() {
                return  matchingMac;
            }

            public Ip4Address getIp() {
                return ip;
            }


        }

    }

*/

}


