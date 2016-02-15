package org.onosproject.phase1;

import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.driver.extensions.OfdpaMatchVlanVid;
import org.onosproject.driver.extensions.OfdpaSetVlanVid;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.*;
import org.onosproject.net.group.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

/**
* Created by nick on 2/11/16.
*/


public class NetworkElements{

    static final Logger log = LoggerFactory.getLogger(NetworkElements.class);


    static final int ACL_TABLE = 60;
    static final int PORT_TABLE = 0;
    static final int VLAN_TABLE = 10;
    static final int TMAC_TABLE = 20;
    static final int VLAN1_TABLE = 11;
    static final int BRIDGING_TABLE = 50;




    //Internet ports, connections to the external router (7750)
    static final PortNumber primaryInternet = PortNumber.portNumber(153);
    static final PortNumber secondaryInternet = PortNumber.portNumber(157);



    static final VlanId internalInternetVlan = VlanId.vlanId((short) 500);
    static final VlanId primaryInternetVlan = VlanId.vlanId((short) 501);
    static final VlanId secondaryInternetVlan = VlanId.vlanId((short) 502);


    private FlowRuleService flowRuleService;
    private GroupService groupService;

    private ApplicationId appId;
    private DeviceId deviceId;

    private List<NetworkElement> elements;
    private GroupId serverFloodGroup = null;

    public NetworkElements(FlowRuleService flowRuleService, GroupService groupService, ApplicationId appId, DeviceId deviceId){
        elements = new LinkedList<>();
        this.flowRuleService = flowRuleService;
        this.groupService = groupService;
        this.appId = appId;
        this.deviceId = deviceId;
    }

    public void addElement(NetworkElement newElement){
        elements.add(newElement);
        log.debug("New element added");
        //TODO: update  flows and flood group
    }

    private List<Olt> getOlts(){

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

    private QuaggaInstance getQuagga(boolean primary){
        for(NetworkElement element : elements){
            if(element instanceof QuaggaInstance){
                QuaggaInstance quagga = (QuaggaInstance) element;
                if(quagga.isPrimary() == primary){
                    return quagga;
                }
            }
        }
        return null;
    }

    private QuaggaInstance getPrimaryQuagga(){
        return getQuagga(true);
    }

    private QuaggaInstance getSecondaryQuagga(){
        return getQuagga(false);
    }

    public void update(){
        updateFloodGroup(deviceId);
        internetFlows(deviceId);
        floodFlow(deviceId);
        innerNetworkFlows(deviceId);
        lanFlows(deviceId);
    }

    private void updateFloodGroup(DeviceId deviceId){

        //TODO: really update instead of removing and adding the new group

        Integer floodGroupId =  ((3 << 28) | ((internalInternetVlan.toShort()) << 16));
        final GroupKey floodGroupkey = new DefaultGroupKey(ByteBuffer.allocate(4).putInt(floodGroupId).array());

        if(serverFloodGroup != null){
            groupService.removeGroup(deviceId, floodGroupkey, appId);
        }


        List<GroupBucket> floodBuckets = new LinkedList<>();

        for(NetworkElement element : elements){

            if(element instanceof VsgServer || element instanceof QuaggaInstance) {

                GroupBucket floodBucket = DefaultGroupBucket.createAllGroupBucket(DefaultTrafficTreatment.builder().group(GroupFinder.getL2Interface(element.getPortNumber(), internalInternetVlan, deviceId )).build());
                floodBuckets.add(floodBucket);
                vlanTableFlows(element.getPortNumber(), internalInternetVlan, deviceId);
            }
        }

        GroupDescription floodGroupDescription = new DefaultGroupDescription(deviceId,
                GroupDescription.Type.ALL,
                new GroupBuckets(floodBuckets),
                floodGroupkey,
                floodGroupId,
                appId);

        groupService.addGroup(floodGroupDescription);

        serverFloodGroup = groupService.getGroup(deviceId, floodGroupkey).id();

    }

    private void connectOltToServer(Olt olt, VsgServer server, DeviceId device){

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

    }



    private void oneWayAclVlanFlow(VlanId vlanId, PortNumber fromPort, PortNumber toPort, DeviceId deviceId, int priority, boolean popVlan) {


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

        flowRuleService.applyFlowRules(rule.build());

    }

    private void twoWayVlanFlow(VlanId vlanId,PortNumber port1, PortNumber port2,  DeviceId deviceId, int priority, boolean popVlan) {

        //Vlan table flows
        vlanTableFlows(port1, vlanId, deviceId);
        vlanTableFlows(port2, vlanId, deviceId);

        //ACL table flows
        oneWayAclVlanFlow(vlanId, port1, port2, deviceId, priority, popVlan);
        oneWayAclVlanFlow(vlanId, port2, port1, deviceId, priority+1, popVlan);

    }




    private void vlanTableFlows(PortNumber port, VlanId vlanId, DeviceId deviceId){

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

        flowRuleService.applyFlowRules(vlanTableRule.build());

    }

    private void untaggedPacketsTagging(PortNumber port, VlanId vlanId, DeviceId deviceId){

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

    private void internetFlows(DeviceId deviceId){

        //////Internet to server

        //Tagging the untagged traffic from the internet
        untaggedPacketsTagging(primaryInternet, primaryInternetVlan, deviceId);
        untaggedPacketsTagging(secondaryInternet, secondaryInternetVlan, deviceId);

        PortNumber primaryQuagga = getPrimaryQuagga().getPortNumber();
        PortNumber secondaryQuagga = null;

        if (getSecondaryQuagga() != null) {
            secondaryQuagga = getSecondaryQuagga().getPortNumber();
        }


        //Setting an output
        oneWayAclVlanFlow(primaryInternetVlan, primaryInternet,primaryQuagga , deviceId, 43001, false);
        if(secondaryQuagga != null) {
            oneWayAclVlanFlow(secondaryInternetVlan, secondaryInternet, secondaryQuagga, deviceId, 43002, false);
        }
        ////////


        /////////Server to Internet

        //Vlan table
        vlanTableFlows(primaryQuagga, primaryInternetVlan, deviceId);
        if(secondaryQuagga != null) {
            vlanTableFlows(secondaryQuagga, secondaryInternetVlan, deviceId);
        }

        //ACL table
        oneWayAclVlanFlow(primaryInternetVlan, primaryQuagga, primaryInternet, deviceId, 41031, true);
        if(secondaryQuagga != null) {
            oneWayAclVlanFlow(secondaryInternetVlan, secondaryQuagga, secondaryInternet, deviceId, 41032, true);
        }
        /////////


    }

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

    }

    private void floodFlow(DeviceId deviceId){



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
    }

    private void lanFlows(DeviceId deviceId){
        List<Olt> olts = getOlts();
        List<VsgServer> servers = getVsgServers();
        for(Olt olt : olts){
            for(VsgServer vsgServer : servers){
                connectOltToServer(olt, vsgServer, deviceId);
            }
        }
    }





}
