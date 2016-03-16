
package org.onosproject.phase1;

import org.apache.commons.configuration.SystemConfiguration;
import org.apache.felix.scr.annotations.*;



import org.onlab.packet.*;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.GroupId;
import org.onosproject.driver.extensions.OfdpaMatchVlanVid;
import org.onosproject.net.*;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.flow.*;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.group.*;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.*;
import org.onosproject.phase1.config.Phase1AppConfig;
import org.onosproject.phase1.ofdpagroups.GroupFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 * Created by nick on 6/26/15.
 */
@Component(immediate = true)

public class Phase1Component{


    static final Logger log = LoggerFactory.getLogger(Phase1Component.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    private final ConfigFactory<ApplicationId, Phase1AppConfig> cfgAppFactory =
            new ConfigFactory<ApplicationId, Phase1AppConfig>(SubjectFactories.APP_SUBJECT_FACTORY,
                    Phase1AppConfig.class,
                    "phase1") {
                @Override
                public Phase1AppConfig createConfig() {
                    return new Phase1AppConfig();
                }
            };


    static ApplicationId appId;
    static final DeviceId torId = DeviceId.deviceId("of:0000000000000111");
    static MacAddress torMac = MacAddress.valueOf("00:00:00:00:01:11");
    static Ip4Address torIp = Ip4Address.valueOf("29.29.4.1");
    static Ip4Address torGatewayIp = Ip4Address.valueOf("29.29.0.1");
    static Ip4Address primaryUplinkIp = Ip4Address.valueOf("29.29.4.0");

    NetworkElements elements;
    Phase1PacketProcessor processor;
    InternalHostListener hostListener;



    @Activate
    protected void activate() {

        log.debug("trying to activate");


        appId = coreService.registerApplication("org.onosproject.phase1");

        //Initiation

        GroupFinder.initiate(appId, groupService);

        log.debug("GroupFinder initiated");

        elements = new NetworkElements(flowRuleService, groupService, appId, torId, torMac);

        cfgService.registerConfigFactory(cfgAppFactory);

        hostListener = new InternalHostListener();

        hostService.addListener(hostListener);

        //Setup packet processor and flows to intercept the desired ARP
        List<PortNumber> vsgServerPorts = new LinkedList<>();
        vsgServerPorts.add(PortNumber.portNumber(3));
        processor = new Phase1PacketProcessor(vsgServerPorts, NetworkElements.primaryInternet);

        packetService.addProcessor(processor, 2);

        log.debug("processor created and added");



        try {
            for(int i = 0 ; i<3; i++) {
                processor.sendArpRequest(primaryUplinkIp, NetworkElements.primaryInternet, VlanId.NONE);
                Thread.sleep(1000);
            }
        } catch (Exception e){
            log.warn("Exception" , e);
        }

        log.debug("Packet sent to get MAc from uplink");

        //Creation of the network objects
        
        LinkedList<Integer> oltVlans = new LinkedList<>();
        LinkedList<Integer> vm1Vlans = new LinkedList<>();

        oltVlans.add(5);
        vm1Vlans.add(5);

        Olt olt = new Olt(9, oltVlans);

        VsgVm vm1 = new VsgVm(Ip4Address.valueOf("10.255.255.2"), MacAddress.valueOf("52:54:00:E5:28:CF"),vm1Vlans);
        vm1.addVsgs(1, Ip4Address.valueOf("29.29.0.2"), MacAddress.valueOf("52:54:00:3D:29:81"));
        LinkedList<VsgVm> vms1 = new LinkedList<>();
        vms1.add(vm1);


        VsgServer vsgServer1 = new VsgServer(3,MacAddress.valueOf("e4:1d:2d:08:4b:80"), vms1, Ip4Prefix.valueOf("29.29.0.0/22"));



        log.debug("Olt and server objects created");
        log.debug(" Olt : " + olt.toString());
        log.debug("VsgServer 1 : " + vsgServer1.toString());




        elements.addElement(olt);
        elements.addElement(vsgServer1);


        elements.update();

        Phase1AppConfig config = cfgService.getConfig(appId, Phase1AppConfig.class);
        config.testConfig();


        //Limitation for now, single tor
        //TODO: 2 tors


    }


    @Deactivate
    protected void deactivate() {

        flowRuleService.removeFlowRulesById(appId);

        cfgService.unregisterConfigFactory(cfgAppFactory);

        Iterable<Group> appGroups = groupService.getGroups(torId, appId);
        for(Group group : appGroups) {
            groupService.removeGroup(group.deviceId(), group.appCookie(), group.appId());
        }

        log.info("Stopped");
    }



    private class Phase1PacketProcessor implements PacketProcessor {

        private List<PortNumber> vsgServerPorts;
        private PortNumber uplinkPort;

        public Phase1PacketProcessor(List<PortNumber> vsgServerPorts, PortNumber uplinkPort){
            this.vsgServerPorts = vsgServerPorts;
            this.uplinkPort = uplinkPort;


            arpIntercepts(this.vsgServerPorts);
        }


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

        private void arpIntercepts(List<PortNumber> ports){

            //////////Intercept on the internet uplink port
            TrafficSelector.Builder uplinkSelector = DefaultTrafficSelector.builder();
            uplinkSelector.matchInPort(uplinkPort);


            TrafficTreatment toController = DefaultTrafficTreatment.builder().punt().build();


            FlowRule.Builder uplinkArpRule = DefaultFlowRule.builder();
            uplinkArpRule.withSelector(uplinkSelector.build());
            uplinkArpRule.withTreatment(toController);
            uplinkArpRule.withPriority(45000);
            uplinkArpRule.fromApp(appId);
            uplinkArpRule.forTable(NetworkElements.ACL_TABLE);
            uplinkArpRule.makePermanent();
            uplinkArpRule.forDevice(torId);

            flowRuleService.applyFlowRules(uplinkArpRule.build());

            ///////////

            /////////Intercept on the Vsg servers ports (to use the TOR as the gateway

            for(PortNumber port : ports) {

                TrafficSelector.Builder vsgPortSelector = DefaultTrafficSelector.builder();
                vsgPortSelector.matchInPort(port);
                vsgPortSelector.extension(new OfdpaMatchVlanVid(NetworkElements.primaryInternetVlan), torId);

                FlowRule.Builder vsgArpRule = DefaultFlowRule.builder();
                vsgArpRule.withSelector(vsgPortSelector.build());
                vsgArpRule.withTreatment(toController);
                vsgArpRule.withPriority(45000);
                vsgArpRule.fromApp(appId);
                vsgArpRule.forTable(NetworkElements.ACL_TABLE);
                vsgArpRule.makePermanent();
                vsgArpRule.forDevice(torId);

                flowRuleService.applyFlowRules(vsgArpRule.build());

            }


        }

        private void handleArpRequest(PortNumber inPort, Ethernet ethPkt) {
            ARP arpRequest = (ARP) ethPkt.getPayload();
            VlanId vlanId = VlanId.vlanId(ethPkt.getVlanID());

            // ARP request for router. Send ARP reply.
            if (isArpForTor(arpRequest)) {
                sendArpResponse(arpRequest, inPort, vlanId);
            }
        }

        private void handleArpReply(PortNumber inPort, Ethernet ethPkt) {
            ARP arpReply = (ARP) ethPkt.getPayload();


            // ARP reply for router. Process all pending IP packets.
            Ip4Address hostIpAddress = Ip4Address.valueOf(arpReply.getSenderProtocolAddress());
            if(hostIpAddress.equals(primaryUplinkIp)) {
                MacAddress uplinkMac = MacAddress.valueOf(arpReply.getSenderHardwareAddress());
                elements.setMacUplink(uplinkMac);
                log.info("Uplink MAC found : "  +uplinkMac.toString());
            }
        }


        private boolean isArpForTor(ARP arpMsg) {
            Ip4Address targetProtocolAddress = Ip4Address.valueOf(
                    arpMsg.getTargetProtocolAddress());
            return (targetProtocolAddress.equals(torIp)||targetProtocolAddress.equals(torGatewayIp));
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
            OutboundPacket outPacket = new DefaultOutboundPacket(torId,
                    treatment.build(), ByteBuffer.wrap(eth.serialize()));

            packetService.emit(outPacket);
        }

        private void sendArpResponse(ARP arpRequest, PortNumber dstPort, VlanId vlanId) {
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

            Ethernet eth = new Ethernet();
            eth.setDestinationMACAddress(arpRequest.getSenderHardwareAddress())
                    .setSourceMACAddress(torMac)
                    .setEtherType(Ethernet.TYPE_ARP)
                    .setPayload(arpReply);

            if(!vlanId.equals(VlanId.NONE)){
                eth.setVlanID(vlanId.toShort());
            }


            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            treatment.setOutput(dstPort);
            OutboundPacket outPacket = new DefaultOutboundPacket(torId,
                    treatment.build(), ByteBuffer.wrap(eth.serialize()));

            packetService.emit(outPacket);
        }
    }



    public void linkPorts(int port1, int port2){

        int tunnelVlan = 17;

        elements.untaggedPacketsTagging(PortNumber.portNumber(port1), VlanId.vlanId((short)tunnelVlan), torId);
        elements.untaggedPacketsTagging(PortNumber.portNumber(port2), VlanId.vlanId((short)tunnelVlan), torId);

        TrafficSelector.Builder selector1 = DefaultTrafficSelector.builder();
        selector1.matchInPort(PortNumber.portNumber(port1));

        TrafficTreatment.Builder outpoutTo2 = DefaultTrafficTreatment.builder();
        outpoutTo2.group(GroupFinder.getL2Interface(port2, tunnelVlan, true, torId));

        FlowRule.Builder rule1to2 = DefaultFlowRule.builder();
        rule1to2.withSelector(selector1.build());
        rule1to2.withTreatment(outpoutTo2.build());
        rule1to2.withPriority(51000);
        rule1to2.forDevice(torId);
        rule1to2.forTable(NetworkElements.ACL_TABLE);
        rule1to2.makePermanent();

        flowRuleService.applyFlowRules(rule1to2.build());

        TrafficSelector.Builder selector2 = DefaultTrafficSelector.builder();
        selector2.matchInPort(PortNumber.portNumber(port2));

        TrafficTreatment.Builder outpoutTo1 = DefaultTrafficTreatment.builder();
        outpoutTo2.group(GroupFinder.getL2Interface(port1, tunnelVlan, true, torId));

        FlowRule.Builder rule2to1 = DefaultFlowRule.builder();
        rule2to1.withSelector(selector2.build());
        rule2to1.withTreatment(outpoutTo1.build());
        rule2to1.withPriority(51000);
        rule2to1.forDevice(torId);
        rule2to1.forTable(NetworkElements.ACL_TABLE);
        rule2to1.makePermanent();

        flowRuleService.applyFlowRules(rule2to1.build());



    }

    private class InternalHostListener implements HostListener {

        private void readInitialHosts() {
            hostService.getHosts().forEach(host -> {
                addFlow(host);
            });
        }

        private void addFlow(Host host){
            MacAddress mac = host.mac();
            VlanId vlanId = host.vlan();
            DeviceId deviceId = host.location().deviceId();
            PortNumber port = host.location().port();

            //L2 flow to each known host
            elements.bridgingTableFlow(port, vlanId, mac, deviceId);
        }

        private  void removeFlow(Host host){

            MacAddress mac = host.mac();
            VlanId vlanId = host.vlan();
            DeviceId deviceId = host.location().deviceId();
            PortNumber port = host.location().port();

            //Remove L2 flow to each known host
            elements.removeBridgingTableFlow(port, vlanId, mac, deviceId);
        }

        @Override
        public void event(HostEvent event) {

            switch (event.type()) {
                case HOST_ADDED:
                    addFlow(event.subject());
                    break;
                case HOST_MOVED:
                    removeFlow(event.prevSubject());
                    addFlow(event.subject());
                    break;
                case HOST_REMOVED:
                    removeFlow(event.subject());
                    break;
                case HOST_UPDATED:
                    removeFlow(event.prevSubject());
                    addFlow(event.subject());
                    break;
                default:
                    log.warn("Unsupported host event type: {}", event.type());
                    break;
            }
        }
    }






}


