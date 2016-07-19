
package org.onosproject.noviaggswitch;


import org.apache.felix.scr.annotations.*;



import org.onlab.packet.*;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.driver.extensions.NoviflowMatchVni;
import org.onosproject.driver.extensions.NoviflowPopVxLan;
import org.onosproject.driver.extensions.NoviflowSetVxLan;
import org.onosproject.driver.extensions.ofmessages.OFNoviflowVniExperimenterMsg;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.*;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.*;
import org.onosproject.net.meter.*;
import org.onosproject.net.packet.*;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;


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
    protected OpenFlowController openFlowController;





    static ApplicationId appId;

    static final DeviceId deviceId = DeviceId.deviceId("of:000000223d5a00d9");


    private static MacAddress uplinkMac = MacAddress.valueOf("a0:36:9f:27:88:f0");
    private static Ip4Address uplinkIp = Ip4Address.valueOf("10.1.4.1");

    private static MacAddress torMac = MacAddress.valueOf("68:05:33:44:55:66");
    private static Ip4Address torIp = Ip4Address.valueOf("10.20.1.1");
    private static Ip4Address torRoutedInterfaceIp = Ip4Address.valueOf("10.1.4.0");

    private PortNumber bngPort;

    private PacketProcessor processor;






    @Activate
    protected void activate() {

        log.debug("trying to activate");
        appId = coreService.registerApplication("org.onosproject.noviaggswitch");

        OFNoviflowVniExperimenterMsg vniMatchSetup = new OFNoviflowVniExperimenterMsg(0);
        vniMatchSetup.send(openFlowController, deviceId);

        processor = new NoviBngPacketProcessor();

        packetService.addProcessor(processor, 1);

        bngPort =  PortNumber.portNumber(2);

        arpIntercept(torIp);
        icmpIntercept(torIp);

        Random rand = new Random();

        addAccessDevice(5, 5000, rand.nextInt(), "10.20.1.2", "68:05:ca:30:00:68", "10.20.1.1", "68:05:33:44:55:66", true);
        addAccessDevice(6, 5001, rand.nextInt(), "10.20.1.2", "68:05:ca:30:00:68", "10.20.1.1", "68:05:33:44:55:66", false);
        addAccessDevice(7, 5002, rand.nextInt(), "10.20.1.2", "68:05:ca:30:00:68", "10.20.1.1", "68:05:33:44:55:66", false);






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

    private void addAccessDevice(int port, int vni, int udpPort, String bngVxlanIp, String bngVxlanMac, String switchVxlanIp, String switchVxlanMac, boolean twoWay) {

        try {
            accessToBng(PortNumber.portNumber(port), vni, udpPort, Ip4Address.valueOf(bngVxlanIp), MacAddress.valueOf(bngVxlanMac), Ip4Address.valueOf(switchVxlanIp), MacAddress.valueOf(switchVxlanMac));
            if(twoWay) {
                bngToAccess(PortNumber.portNumber(port), vni, Ip4Address.valueOf(bngVxlanIp));
            }
        } catch(Exception e) {
            log.warn("Exception", e);
        }

    }

    private void accessToBng(PortNumber port, int vni, int udpPort, Ip4Address bngVxlanIp, MacAddress bngVxlanMac, Ip4Address switchVxlanIp, MacAddress switchVxlanMac){

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchInPort(port);


        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.extension(new NoviflowSetVxLan(switchVxlanMac,bngVxlanMac, switchVxlanIp, bngVxlanIp, udpPort, vni), deviceId);
        treatment.setOutput(bngPort);

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(2000);
        rule.forTable(0);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        flowRuleService.applyFlowRules(rule.build());

    }


    private void bngToAccess(PortNumber port, int vni, Ip4Address switchVxlanIp) {

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchInPort(bngPort);
        selector.extension(new NoviflowMatchVni(vni), deviceId);


        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.extension(new NoviflowPopVxLan(), deviceId);
        treatment.setOutput(port);

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(1500);
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
        rule.withPriority(10000);
        rule.forTable(0);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        flowRuleService.applyFlowRules(rule.build());
    }

    private void icmpIntercept(Ip4Address respondFor) {

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchIPDst(respondFor.toIpPrefix());
        //selector.matchIPProtocol(IPv4.PROTOCOL_ICMP);


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



    private class NoviBngPacketProcessor implements PacketProcessor {


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


            // ARP reply for router. Process all pending IP packets.
            Ip4Address hostIpAddress = Ip4Address.valueOf(arpReply.getSenderProtocolAddress());
            if(hostIpAddress.equals(uplinkIp)) {
                MacAddress newUplinkMac = MacAddress.valueOf(arpReply.getSenderHardwareAddress());
                //Check if the one previously on file was different
                if(uplinkMac != null && !newUplinkMac.equals(uplinkMac)){
                    //The Mac has changed, need to refresh the flows
                }
                log.info("Uplink MAC found : "  +newUplinkMac.toString());
            }
        }

        private void handlePingRequest(PortNumber inPort, Ethernet ethPkt) {

            IPv4 ipPkt = (IPv4) ethPkt.getPayload();


            Ip4Address requestIpAddress = Ip4Address.valueOf(ipPkt.getDestinationAddress());
            if(requestIpAddress.equals(torIp)) {

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
                    .setSourceMACAddress(torMac)
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
            return (targetProtocolAddress.equals(torIp)||targetProtocolAddress.equals(torRoutedInterfaceIp));
        }


        public void sendArpRequest(IpAddress targetAddress, PortNumber dstPort, VlanId vlanId) {


            ARP arpRequest = new ARP();
            arpRequest.setHardwareType(ARP.HW_TYPE_ETHERNET)
                    .setProtocolType(ARP.PROTO_TYPE_IP)
                    .setHardwareAddressLength(
                            (byte) Ethernet.DATALAYER_ADDRESS_LENGTH)
                    .setProtocolAddressLength((byte) Ip4Address.BYTE_LENGTH)
                    .setOpCode(ARP.OP_REQUEST)
                    .setSenderHardwareAddress(torMac.toBytes())
                    .setTargetHardwareAddress(MacAddress.ZERO.toBytes())
                    .setSenderProtocolAddress(torIp.toOctets())
                    .setTargetProtocolAddress(targetAddress.toOctets());

            Ethernet eth = new Ethernet();
            eth.setDestinationMACAddress(MacAddress.BROADCAST.toBytes())
                    .setSourceMACAddress(torMac)
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
                    .setSenderHardwareAddress(torMac.toBytes())
                    .setSenderProtocolAddress(arpRequest.getTargetProtocolAddress())
                    .setTargetHardwareAddress(arpRequest.getSenderHardwareAddress())
                    .setTargetProtocolAddress(arpRequest.getSenderProtocolAddress());


            //Modify the original request and send it back : It keeps the proper vlan tagging
            eth.setDestinationMACAddress(arpRequest.getSenderHardwareAddress())
                    .setSourceMACAddress(torMac)
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
    }



}


