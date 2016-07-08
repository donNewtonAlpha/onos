
package org.onosproject.novibng;


import org.apache.felix.scr.annotations.*;



import org.onlab.packet.*;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.driver.extensions.NoviflowPopVxLan;
import org.onosproject.driver.extensions.NoviflowSetVxLan;
import org.onosproject.driver.extensions.OfdpaMatchVlanVid;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Created by nick on 6/26/15.
 */
@Component(immediate = true)

public class NoviBngComponent {


    static final Logger log = LoggerFactory.getLogger(NoviBngComponent.class);

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



    static ApplicationId appId;

    static final DeviceId deviceId = DeviceId.deviceId("of:000000223d5a00d9");

    private static PortNumber uplinkPort = PortNumber.portNumber(8);
    private static MacAddress uplinkMac = MacAddress.valueOf("a0:36:9f:27:88:f0");
    private static Ip4Address uplinkIp = Ip4Address.valueOf("10.1.4.1");

    private static MacAddress torMac = MacAddress.valueOf("11:22:33:44:55:66");
    private static Ip4Address torIp = Ip4Address.valueOf("20.20.0.1");
    private static Ip4Address torRoutedInterfaceIp = Ip4Address.valueOf("10.1.4.0");

    private LinkedList<Integer> sTagsEnabled = new LinkedList<>();






    @Activate
    protected void activate() {

        log.debug("trying to activate");
        appId = coreService.registerApplication("org.onosproject.novibng");

        acl();
        arpIntercept();
        voIpDetection();
        downstreamTrafficHandling();

        for(int i = 2; i < 4 ; i++) {

            for(int j = 2; j < 4 ; j++) {

                addCustomer(i, j, 5, (2*i+3*j)*1000);

            }
        }



        log.info("NoviFlow BNG activated");

    }


    @Deactivate
    protected void deactivate() {

        flowRuleService.removeFlowRulesById(appId);


        Iterable<Group> appGroups = groupService.getGroups(deviceId, appId);
        for(Group group : appGroups) {
            groupService.removeGroup(group.deviceId(), group.appCookie(), group.appId());
        }



        log.info("Stopped");
    }

    private Ip4Address tagsToIpMatching(int sTag, int cTag) {

        return Ip4Address.valueOf("20.20." + sTag + "." + cTag);
    }

    private boolean isStagEnabled(int sTag) {
        return sTagsEnabled.contains(sTag);
    }

    private void addCustomer(int sTag, int cTag, int port, int kbpsRate) {

        //Upstream

        if(!isStagEnabled(sTag)){
            sTagMatch(port, sTag);
        }

        meterAssignmentAndOut(sTag, cTag, kbpsRate);


        //Downstream

        downstreamCtag(sTag, cTag, port);
        downstreamStag(sTag, cTag, port);


    }

    private void downstreamCtag(int sTag, int cTag, int port) {

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchIPDst(tagsToIpMatching(sTag, cTag).toIpPrefix());


        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.pushVlan();
        treatment.setVlanId(VlanId.vlanId((short) cTag));
        treatment.transition(11);

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(2000);
        rule.forTable(10);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        flowRuleService.applyFlowRules(rule.build());


    }

    private void downstreamStag(int sTag, int cTag, int port) {

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchIPDst(tagsToIpMatching(sTag, cTag).toIpPrefix());


        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.pushVlan();
        treatment.setVlanId(VlanId.vlanId((short) sTag));
        treatment.setQueue(3);
        treatment.setOutput(PortNumber.portNumber(port));


        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(2000);
        rule.forTable(11);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        flowRuleService.applyFlowRules(rule.build());


    }

    private void downstreamTrafficHandling(){

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchInPort(uplinkPort);


        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.transition(10);


        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(5000);
        rule.forTable(0);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        flowRuleService.applyFlowRules(rule.build());
    }

    private void sTagMatch(int port, int sTag){


        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchInPort(PortNumber.portNumber(port));
        selector.matchVlanId(VlanId.vlanId((short) sTag));


        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.writeMetadata(sTag, 0xffffffff);
        treatment.popVlan();
        treatment.transition(1);

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(100 + sTag);
        rule.forTable(0);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        flowRuleService.applyFlowRules(rule.build());

    }

    private void aclDropFlow(Ip4Address dstIP, Ip4Address srcIp){

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        if(dstIP != null) {
            selector.matchIPDst(dstIP.toIpPrefix());
        }
        if(srcIp != null) {
            selector.matchIPSrc(srcIp.toIpPrefix());
        }


        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.drop();

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(200);
        rule.forTable(1);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        flowRuleService.applyFlowRules(rule.build());
    }

    private void acl(){

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.transition(3);

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(1);
        rule.forTable(1);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        flowRuleService.applyFlowRules(rule.build());

        aclDropFlow(Ip4Address.valueOf("12.34.56.78"), null);

    }

    private void voIpDetection() {

        //TODO

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.transition(4);

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(1);
        rule.forTable(3);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        flowRuleService.applyFlowRules(rule.build());

    }

    private void meterAssignmentAndOut(int sTag, int cTag, int kbps){

        //create a new meter
        MeterRequest.Builder meter = DefaultMeterRequest.builder();
        meter.forDevice(deviceId);
        meter.fromApp(appId);
        meter.withUnit(Meter.Unit.KB_PER_SEC);
        Band.Builder band = DefaultBand.builder();
        band.withRate(kbps);
        band.ofType(Band.Type.DROP);
        meter.withBands(Collections.singleton(band.build()));

        Meter finalMeter = meterService.submit(meter.add());

        //create the flow

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchVlanId(VlanId.vlanId((short) cTag));
        selector.matchMetadata(sTag);

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.popVlan();
        treatment.setEthDst(uplinkMac);
        treatment.meter(finalMeter.id());
        treatment.setOutput(uplinkPort);
        treatment.setQueue(3);


        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(1);
        rule.forTable(4);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        flowRuleService.applyFlowRules(rule.build());

    }

    private void arpIntercept() {
        
        //TODO : make a table rule and integrate IGMP  (table 20) ?

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_ARP);


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

                    log.debug("ARP packet received by phase 1");

                    ARP arp = (ARP) payload;

                    PortNumber inPort = pkt.receivedFrom().port();

                    if (arp.getOpCode() == ARP.OP_REQUEST) {
                        log.debug("It is an ARP request");
                        handleArpRequest(inPort, ethPkt);
                    } else {
                        log.debug("It is an ARP reply");
                        handleArpReply(inPort, ethPkt);
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
            

            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            treatment.setOutput(dstPort);
            OutboundPacket outPacket = new DefaultOutboundPacket(deviceId,
                    treatment.build(), ByteBuffer.wrap(eth.serialize()));

            packetService.emit(outPacket);
        }
    }



}


