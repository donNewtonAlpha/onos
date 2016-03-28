package org.onosproject.phase1;

import org.onlab.packet.*;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.driver.extensions.OfdpaMatchVlanVid;
import org.onosproject.driver.extensions.OfdpaSetVlanVid;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.*;
import org.onosproject.net.group.*;
import org.onosproject.phase1.config.Phase1AppConfig;
import org.onosproject.phase1.config.VlanCrossconnect;
import org.onosproject.phase1.ofdpagroups.GroupFinder;
import org.onosproject.phase1.ofdpagroups.L2MulticastGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
* Created by nick on 2/11/16.
*/


public class NetworkElements{

    static final Logger log = LoggerFactory.getLogger(NetworkElements.class);


    static final int ACL_TABLE = 60;
    static final int VLAN_TABLE = 10;
    static final int TMAC_TABLE = 20;
    static final int UNICAST_ROUTING_TABLE = 30;
    static final int BRIDGING_TABLE = 50;




    //Internet ports, connections to the external router (7750)
    static PortNumber primaryInternet = null;
    static PortNumber secondaryInternet = null;
    static Ip4Address torIp = null;
    static Ip4Address torGatewayIp = null;
    static Ip4Address primaryUplinkIp = null;
    static VlanId internalInternetVlan = null;
    static List<VlanCrossconnect> vlanCrossconnects = new LinkedList<>();
    static List<PortNumber> internalWanPorts = null;





    private FlowRuleService flowRuleService;
    private ApplicationId appId;
    private GroupService groupService;
    private DeviceId deviceId;
    MacAddress torMac;
    private MacAddress primaryUplinkMac;
    private Phase1Component phase1;

    private List<NetworkElement> elements;
    private FlowRule floodRule = null;

    private long timer = 0;

    public NetworkElements(FlowRuleService flowRuleService, GroupService groupService, ApplicationId appId, DeviceId deviceId, MacAddress mac, Phase1Component component){
        elements = new LinkedList<>();
        this.flowRuleService = flowRuleService;
        this.groupService = groupService;
        this.appId = appId;
        this.deviceId = deviceId;
        this.torMac = mac;
        this.phase1 = component;
    }

    public void addElement(NetworkElement newElement){
        elements.add(newElement);
        log.debug("New element added");
        //TODO: update  flows and flood group
    }



    public void removeFlows(List<FlowRule> flows){
        for(FlowRule flow : flows){
            flowRuleService.removeFlowRules(flow);
        }
    }

    public void addFlows(List<FlowRule> flows){
        for(FlowRule flow : flows){
            flowRuleService.applyFlowRules(flow);
        }
    }

    public synchronized void update(Phase1AppConfig config){

        log.info("Updating flows based on config");

        if(config == null){
            log.info("No config yet");
            return;
        }
        if(!config.isValid()){
            log.info("Config not valid");
            return;
        }


        ////////// Vlan crossconnects
        //Find the difference between the Vlan crossconnects and the config
        List<VlanCrossconnect> vlanCrossconnectsToAdd = new LinkedList<>(config.vlanCrossconnects());
        List<VlanCrossconnect> vlanCrossconnectsToRemove = new LinkedList<>();
        for(VlanCrossconnect vcc : vlanCrossconnects){
            log.info("Current VlanCrossConnect : " + vcc.toString());
            if(!vlanCrossconnectsToAdd.remove(vcc)){
                vlanCrossconnectsToRemove.add(vcc);
            }
        }

        //Get the matching flows and remove the old and add the new


        for(VlanCrossconnect vcc : vlanCrossconnectsToRemove){
            log.info("VlanCrossconnect to remove : " + vcc.toString());
            removeFlows(vlanCrossConnectFlows(vcc, deviceId));
        }
        for(VlanCrossconnect vcc : vlanCrossconnectsToAdd){
            log.info("VlanCrossconnect to add : " + vcc.toString());
            addFlows(vlanCrossConnectFlows(vcc, deviceId));
        }


        //Update with the latest config
        vlanCrossconnects = config.vlanCrossconnects();

        ///////////////////

        ///////// Gateway IP
        torGatewayIp = config.gatewayIp();
        //////////////

        /////////////Tor side uplink IP
        torIp = config.primaryFabricIp();
        ////////////

        //////////////Uplink IP
        boolean redetectUplinkMac = false;
        if(!(primaryUplinkIp != null && primaryUplinkIp.equals(config.primaryUplinkIp()))){
            // New primary uplink ip, redetect the matching mac address
            redetectUplinkMac = true;
            if(primaryUplinkIp != null) {
                //It has changed, remove old flows
                removeFlows(uplinkFlows(deviceId, primaryInternet, primaryUplinkMac));
            }
            primaryUplinkIp = config.primaryUplinkIp();
        }
        /////////////////////

        //////////////// Internal Internet Vlan
        if(!(internalInternetVlan != null && internalInternetVlan.equals(config.internalInternetVlan()))){
            //New or changed
            if(internalInternetVlan != null){
                //It was changed
                log.warn("Why did you change that ???????????????? Are you crazy ??? This is not implemented yet");
                //TODO : remove old flows, replace them with new flows
            }
            internalInternetVlan = config.internalInternetVlan();

        }
        ///////////////////


        boolean packetProcessorToReset = false;
        ///////////////Uplink Port
        if(!(primaryInternet != null && primaryInternet.equals(config.primaryUplinkPort()))){
            if(!redetectUplinkMac) {
                redetectUplinkMac = true;
                if(primaryInternet != null) {
                    removeFlows(uplinkFlows(deviceId, primaryInternet, primaryUplinkMac));
                }
            }
            primaryInternet = config.primaryUplinkPort();
            packetProcessorToReset = true;

        }

        ////////////////Internal Wan ports
        if(!(internalInternetVlan != null && internalInternetVlan.equals(config.internalWanPorts()))){
            //New or changed

            internalWanPorts = config.internalWanPorts();
            updateFlooding(internalWanPorts, internalInternetVlan, deviceId);
            log.info("Flooding Rule updated");
            packetProcessorToReset = true;
        }
        ///////////////

        if(packetProcessorToReset) {
            phase1.resetPacketProcessor(internalWanPorts, primaryInternet);
            log.info("Phase 1 Packet processor created/reset");
        }

        if(redetectUplinkMac){
            phase1.requestUplinkMac(primaryUplinkIp, primaryInternet);
        }
        //////////////////////



    }


    private List<FlowRule> vlanCrossConnectFlows(VlanCrossconnect vlanCrossconnect, DeviceId deviceId){
        PortNumber port1 = vlanCrossconnect.getPorts().get(0);
        PortNumber port2 = vlanCrossconnect.getPorts().get(1);

        return twoWayVlanFlow(vlanCrossconnect.getVlanId(), port1, port2, deviceId, 41000, false);
    }



    private FlowRule oneWayAclVlanFlow(VlanId vlanId, PortNumber fromPort, PortNumber toPort, DeviceId deviceId, int priority, boolean popVlan) {


        //ACL table Flow
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchInPort(fromPort);
        selector.extension(new OfdpaMatchVlanVid(vlanId), deviceId);


        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.group(GroupFinder.getL2Interface(toPort, vlanId, popVlan, deviceId));


        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(priority);
        rule.fromApp(appId);
        rule.forTable(ACL_TABLE);
        rule.makePermanent();
        rule.forDevice(deviceId);

        return rule.build();

    }

    public List<FlowRule> twoWayVlanFlow(VlanId vlanId, PortNumber port1, PortNumber port2,  DeviceId deviceId, int priority, boolean popVlan) {

        List<FlowRule> flows = new LinkedList<>();

        //Vlan table flows
        flows.add(vlanTableFlows(port1, vlanId, deviceId));
        flows.add(vlanTableFlows(port2, vlanId, deviceId));

        //ACL table flows
        flows.add(oneWayAclVlanFlow(vlanId, port1, port2, deviceId, priority, popVlan));
        flows.add(oneWayAclVlanFlow(vlanId, port2, port1, deviceId, priority, popVlan));

        return flows;

    }




    private FlowRule vlanTableFlows(PortNumber port, VlanId vlanId, DeviceId deviceId){

        TrafficSelector.Builder vlanTableSelector = DefaultTrafficSelector.builder();
        vlanTableSelector.matchInPort(port);
        vlanTableSelector.extension(new OfdpaMatchVlanVid(vlanId), deviceId);

        TrafficTreatment.Builder vlanTableTreatment = DefaultTrafficTreatment.builder();
        vlanTableTreatment.transition(TMAC_TABLE);

        FlowRule.Builder vlanTableRule = DefaultFlowRule.builder();
        vlanTableRule.withSelector(vlanTableSelector.build());
        vlanTableRule.withTreatment(vlanTableTreatment.build());
        vlanTableRule.withPriority(8);
        vlanTableRule.forTable(VLAN_TABLE);
        vlanTableRule.fromApp(appId);
        vlanTableRule.forDevice(deviceId);
        vlanTableRule.makePermanent();

        return vlanTableRule.build();

    }



    public void bridgingTableFlow(PortNumber outPort, VlanId vlanId, MacAddress dstMac, DeviceId deviceId){

        flowRuleService.applyFlowRules(buildBridgingTableFlow(outPort, vlanId, dstMac, deviceId));

    }

    public void removeBridgingTableFlow(PortNumber outPort, VlanId vlanId, MacAddress dstMac, DeviceId deviceId){

        flowRuleService.removeFlowRules(buildBridgingTableFlow(outPort, vlanId, dstMac, deviceId));

    }

    private FlowRule buildBridgingTableFlow(PortNumber outPort, VlanId vlanId, MacAddress dstMac, DeviceId deviceId){

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.extension(new OfdpaMatchVlanVid(vlanId), deviceId);
        selector.matchEthDst(dstMac);

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.group(GroupFinder.getL2Interface(outPort, vlanId, false, deviceId));
        treatment.transition(ACL_TABLE);

        FlowRule.Builder flow = DefaultFlowRule.builder();
        flow.withSelector(selector.build());
        flow.withTreatment(treatment.build());
        flow.withPriority(2000);
        flow.forTable(BRIDGING_TABLE);
        flow.fromApp(appId);
        flow.forDevice(deviceId);
        flow.makePermanent();

        return flow.build();
    }

    void untaggedPacketsTagging(PortNumber port, VlanId vlanId, DeviceId deviceId){

        vlanTableFlows(port, vlanId, deviceId);

        TrafficSelector.Builder taggingSelector = DefaultTrafficSelector.builder();
        taggingSelector.matchInPort(port);
        //taggingSelector.extension(new OfdpaMatchVlanVid(VlanId.vlanId((short) 0)), deviceId);
        taggingSelector.extension(new OfdpaMatchVlanVid(VlanId.NONE), deviceId);

        TrafficTreatment.Builder taggingTreatment = DefaultTrafficTreatment.builder();
        taggingTreatment.extension(new OfdpaSetVlanVid(vlanId), deviceId);
        taggingTreatment.transition(TMAC_TABLE);

        FlowRule.Builder taggingRule = DefaultFlowRule.builder();
        taggingRule.withSelector(taggingSelector.build());
        taggingRule.withTreatment(taggingTreatment.build());
        taggingRule.makePermanent();
        taggingRule.withPriority((short) port.toLong());
        taggingRule.fromApp(appId);
        taggingRule.forDevice(deviceId);
        taggingRule.forTable(VLAN_TABLE);

        flowRuleService.applyFlowRules(taggingRule.build());



    }

    private void innerNetworkMacFlow(/*PortNumber inPort,*/ PortNumber outPort, MacAddress mac, DeviceId deviceId){

        //ACL table Flow
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        //selector.matchInPort(inPort);
        selector.extension(new OfdpaMatchVlanVid(internalInternetVlan), deviceId);
        selector.matchEthDst(mac);

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.group(GroupFinder.getL2Interface(outPort, internalInternetVlan, false, deviceId));


        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(47500);
        rule.fromApp(appId);
        rule.forTable(ACL_TABLE);
        rule.makePermanent();
        rule.forDevice(deviceId);

        flowRuleService.applyFlowRules(rule.build());



    }

    public void tMacTableFlowUnicastRouting(MacAddress dstMac, int priority){
        //TMAC table Flow
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchEthDst(dstMac);

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.transition(UNICAST_ROUTING_TABLE);


        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(priority);
        rule.fromApp(appId);
        rule.forTable(TMAC_TABLE);
        rule.makePermanent();
        rule.forDevice(deviceId);

        flowRuleService.applyFlowRules(rule.build());
    }

    public FlowRule ipFlow(DeviceId deviceId, Ip4Prefix ipDst, PortNumber outPort, VlanId IncomingVlanId, MacAddress thisHopMac, MacAddress nextHopMac, boolean popVlan, int priority){

        //Unicast routing flow table
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchIPDst(ipDst);

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.transition(ACL_TABLE);
        treatment.deferred();
        treatment.group(GroupFinder.getL3Interface((int)outPort.toLong(), IncomingVlanId.toShort(),thisHopMac, nextHopMac, ipDst, popVlan, deviceId));



        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(priority);
        rule.fromApp(appId);
        rule.forTable(UNICAST_ROUTING_TABLE);
        rule.makePermanent();
        rule.forDevice(deviceId);

        return rule.build();

    }


    private List<FlowRule> uplinkFlows(DeviceId deviceId, PortNumber uplinkPort, MacAddress uplinkMac){

        List<FlowRule> flows = new LinkedList<>();

        flows.add(ipFlow(deviceId, Ip4Prefix.valueOf("0.0.0.0/1"), uplinkPort, internalInternetVlan, torMac, uplinkMac, true, 5));
        flows.add(ipFlow(deviceId, Ip4Prefix.valueOf("128.0.0.0/1"), uplinkPort, internalInternetVlan, torMac, uplinkMac, true, 5));

        return flows;
    }



    public synchronized void setMacUplink(MacAddress mac){

        primaryUplinkMac = mac;
        if(System.currentTimeMillis() - timer > 1000) {
            timer = System.currentTimeMillis();
            addFlows(uplinkFlows(deviceId, primaryInternet, primaryUplinkMac));
        }
    }

    private void updateFlooding(List<PortNumber> ports, VlanId vlanId, DeviceId deviceId) {

        //TODO: update group instead of removing and adding back the flow
        if(floodRule != null){
            //Remove it if it exist

            GroupKey oldKey = L2MulticastGroup.key(vlanId);
            Group floodGroup = groupService.getGroup(deviceId, oldKey);
            List<GroupBucket> currentBuckets = floodGroup.buckets().buckets();
            List<GroupBucket> bucketsToRemove = new LinkedList<>(currentBuckets);
            List<GroupBucket> bucketsToAdd = new LinkedList<>();

            for(PortNumber port : ports) {
                TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                                                .group(GroupFinder.getL2Interface(port,vlanId,false, deviceId)).build();
                boolean toAdd = true;


                for(GroupBucket bucket : currentBuckets) {
                    if(bucket.treatment().equals(treatment)){
                        //The output to this port is already configured, we do not add it
                        toAdd = false;
                        //We do not remove it
                        bucketsToRemove.remove(bucket);
                    }
                }
                if(toAdd) {
                    GroupBucket newBucket = DefaultGroupBucket.createAllGroupBucket(treatment);
                    bucketsToAdd.add(newBucket);
                }
            }

            log.info("Buckets to remove : " + bucketsToRemove.size() + ", Buckets to add : " + bucketsToAdd.size());
            /*boolean random = (new Random()).nextBoolean();
            if(random) {
                log.info("Trying case same key (old)");*/

                groupService.removeBucketsFromGroup(deviceId, oldKey, new GroupBuckets(bucketsToRemove), oldKey, appId);
                groupService.addBucketsToGroup(deviceId, oldKey, new GroupBuckets(bucketsToAdd), oldKey, appId);

          /*  } else {

                log.info("Trying case new key");
                GroupKey newKey = L2MulticastGroup.newKey(vlanId);
                groupService.removeBucketsFromGroup(deviceId, oldKey, new GroupBuckets(bucketsToRemove), newKey, appId);
                groupService.addBucketsToGroup(deviceId, newKey, new GroupBuckets(bucketsToAdd),  L2MulticastGroup.newKey(vlanId), appId);
            }*/
        } else {

            TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
            selector.matchEthDst(MacAddress.BROADCAST);
            selector.extension(new OfdpaMatchVlanVid(vlanId), deviceId);

            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            treatment.group(GroupFinder.getL2Multicast(ports, vlanId, deviceId));
            treatment.transition(ACL_TABLE);

            FlowRule.Builder rule = DefaultFlowRule.builder();
            rule.withSelector(selector.build());
            rule.withTreatment(treatment.build());
            rule.withPriority(2000);
            rule.fromApp(appId);
            rule.forTable(BRIDGING_TABLE);
            rule.makePermanent();
            rule.forDevice(deviceId);

            floodRule = rule.build();

            flowRuleService.applyFlowRules(floodRule);
        }
    }



    /*    private void connectOltToServer(Olt olt, VsgServer server, DeviceId device){

        List<VlanId> oltVlans = olt.getVlanHandled();
        List<VlanId> serverVlans = server.getVlanHandled();

        for(VlanId oltVlan : oltVlans){
            for(VlanId serverVlan : serverVlans){

                if(oltVlan.equals(serverVlan)){
                    //They need to be connected
                    twoWayVlanFlow(oltVlan,olt.getPortNumber(), server.getPortNumber(), device, 41000, false);
                    log.debug("Connecting vlan " + oltVlan +
                            " from " + olt.getPortNumber() +
                            " to " + server.getPortNumber() +
                            " on device : " + device);
                }

            }
        }

    }*/


    /*    private void lanFlows(DeviceId deviceId){
        List<Olt> olts = getOlts();
        List<VsgServer> servers = getVsgServers();
        for(Olt olt : olts){
            for(VsgServer vsgServer : servers){
                connectOltToServer(olt, vsgServer, deviceId);
            }
        }
    }*/

    /*private void internetFlows(DeviceId deviceId){

        //////Flows from uplink to Vsgs

        //Tagging the untagged traffic from the internet
        untaggedPacketsTagging(primaryInternet, internalInternetVlan, deviceId);
        //Treating it as ip unicast traffic, this ToR is now (partially) a router
        tMacTableFlowUnicastRouting(torMac, 10);
        //Setting the Ip flows for all Vsgs
        for(VsgServer server : getVsgServers()){
            for(VsgVm vm : server.getVms()){
                for(Vsg vsg : vm.getVsgs()){
                    log.info("Vsg mac : " + vsg.getWanSideMac() + "Vsg public ip : " + vsg.getPublicIp());
                    ipFlow(vsg.getPublicIp().toIpPrefix().getIp4Prefix(), server.getPortNumber(), internalInternetVlan.toShort(), torMac, vsg.getWanSideMac(),false, 2000);
                }
            }
        }

        //////////////


        ////////Flows from the Vsgs to the Internet

        for(VsgServer server : getVsgServers()){
            //Allowing the internet Vlan on this port
            vlanTableFlows(server.getPortNumber(), internalInternetVlan, deviceId);
            // TMAC flow already in

            //Ip flow to the internet, 2 /1 prefix with low priority

            ipFlow(Ip4Prefix.valueOf("0.0.0.0/1"), primaryInternet, internalInternetVlan.toShort(), torMac, primaryUplinkMac, true, 5);
            ipFlow(Ip4Prefix.valueOf("128.0.0.0/1"), primaryInternet, internalInternetVlan.toShort(), torMac, primaryUplinkMac, true, 5);

        }

    }*/


/*    private void floodFlow(DeviceId deviceId){



        TrafficSelector.Builder floodingSelector = DefaultTrafficSelector.builder();
        floodingSelector.extension(new OfdpaMatchVlanVid(internalInternetVlan), deviceId);
        floodingSelector.matchEthDst(MacAddress.BROADCAST);


        TrafficTreatment.Builder floodTreatment = DefaultTrafficTreatment.builder();
        floodTreatment.group(serverFloodGroup);


        FlowRule.Builder floodRule = DefaultFlowRule.builder();
        floodRule.withSelector(floodingSelector.build());
        floodRule.withTreatment(floodTreatment.build());
        floodRule.makePermanent();
        floodRule.withPriority(46000);
        floodRule.fromApp(appId);
        floodRule.forDevice(deviceId);
        floodRule.forTable(ACL_TABLE);

        flowRuleService.applyFlowRules(floodRule.build());
    }*/

/*
    private void innerNetworkFlows(DeviceId deviceId){

        //TODO : add the incoming port condition ???

        for(NetworkElement element:elements){
            if(element instanceof  VsgServer){
                VsgServer server = (VsgServer) element;
                innerNetworkMacFlow(server.getPortNumber(), server.getMac(), deviceId);
                List<VsgVm> vms = server.getVms();
                for(VsgVm vm : vms){
                    innerNetworkMacFlow(server.getPortNumber(), vm.getMac(), deviceId);
                }
            } else if(element instanceof QuaggaInstance){
                QuaggaInstance quagga = (QuaggaInstance) element;
                innerNetworkMacFlow(quagga.getPortNumber(), quagga.getMac(), deviceId);
            } else if(element instanceof IndependentServer){
                IndependentServer independentServer = (IndependentServer) element;
                innerNetworkMacFlow(independentServer.getPortNumber(), independentServer.getMac(), deviceId);
            }

        }

    }*/



    /*private List<Olt> getOlts(){

        LinkedList<Olt> olts = new LinkedList<>();
        for(NetworkElement element : elements){
            if(element instanceof Olt){
                olts.add((Olt)element);
            }
        }
        return olts;

    }

    private List<VsgServer> getVsgServers(){

        LinkedList<VsgServer> servers = new LinkedList<>();
        for(NetworkElement element : elements){
            if(element instanceof VsgServer){
                servers.add((VsgServer) element);
            }
        }

        return servers;
    }


*/

}
