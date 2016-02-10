
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

public class Phase1Component{


    static final Logger log = LoggerFactory.getLogger(Phase1Component.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;


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



    // Physical port 1, port splitting

    static final PortNumber primaryInternet = PortNumber.portNumber(31);
    static final PortNumber secondaryInternet = PortNumber.portNumber(32);
    static final PortNumber primaryQuagga = PortNumber.portNumber(1);
    static final PortNumber secondaryQuagga = PortNumber.portNumber(1);






    static final VlanId internalInternetVlan = VlanId.vlanId((short) 500);

    static final VlanId primaryInternetVlan = VlanId.vlanId((short) 501);
    static final VlanId secondaryInternetVlan = VlanId.vlanId((short) 502);




    @Activate
    protected void activate() {

        log.debug("trying to activate");
        appId = coreService.registerApplication("org.onosproject.phase1");

        //Initiation

        GroupFinder.initiate(appId, groupService);


        //Creation of the network objects
        
        LinkedList<Integer> oltVlans = new LinkedList<>();
        LinkedList<Integer> serverVlans = new LinkedList<>();
        
        for(int i = 2; i < 10; i++){
            oltVlans.add(i);
            serverVlans.add(i);
        }
        Olt olt = new Olt(3, oltVlans);
        VsgServer vsgServer = new VsgServer(1, serverVlans);

        log.debug("Olt and server objects created");
        log.debug(" Olt : " + olt.toString());
        log.debug("VsgServer : " + vsgServer.toString());


   

        //Limitation for now, single tor
        //TODO: 2 tors
        
        //Connecting the 2
        connectOltToServer(olt, vsgServer, torId);

        internetFlows(torId);


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


    private void connectOltToServer(Olt olt, VsgServer server, DeviceId device){

        List<VlanId> oltVlans = olt.getVlanHandled();
        List<VlanId> serverVlans = server.getVlanHandled();

        for(VlanId oltVlan : oltVlans){
            for(VlanId serverVlan : serverVlans){

                if(oltVlan.equals(serverVlan)){
                    //They need to be connected
                    oltToServerBidirectionnal(oltVlan,olt.getPortNumber(), server.getPortNumber(), device);
                    log.debug("Connecting vlan " + oltVlan +
                            " from " + olt.getPortNumber() + 
                            " to " + server.getPortNumber() +
                            " on device : " + device);
                }

            }
        }

    }


    private void oltToServerBidirectionnal(VlanId vlanId,PortNumber oltPort, PortNumber serverPort,  DeviceId deviceId){


        vlanTableFlows(oltPort, vlanId, deviceId);
        vlanTableFlows(serverPort, vlanId, deviceId);

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




    private void vlanTableFlows(PortNumber port, VlanId vlanId, DeviceId deviceId){

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
        vlanTableHackRule.forDevice(deviceId);
        vlanTableHackRule.makePermanent();

        flowRuleService.applyFlowRules(vlanTableHackRule.build());

    }

    private void untaggedPacketsTagging(PortNumber port, VlanId vlanId, DeviceId deviceId){

        vlanTableFlows(port, vlanId, deviceId);

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
        taggingRule.forDevice(deviceId);
        taggingRule.forTable(VLAN_TABLE);

        flowRuleService.applyFlowRules(taggingRule.build());



    }

    private void internetFlows(DeviceId deviceId){

        //Internet to server

        //Tagging the untagged traffic from the internet
        untaggedPacketsTagging(primaryInternet, primaryInternetVlan, deviceId);
        untaggedPacketsTagging(secondaryInternet, secondaryInternetVlan, deviceId);

        //Setting an output


        TrafficSelector.Builder primaryInternetSelector = DefaultTrafficSelector.builder();
        primaryInternetSelector.matchInPort(primaryInternet);
        primaryInternetSelector.matchVlanId(primaryInternetVlan);

        TrafficTreatment.Builder primaryInternetTreatment = DefaultTrafficTreatment.builder();
        primaryInternetTreatment.group(GroupFinder.getL2Interface(primaryQuagga, primaryInternetVlan, deviceId));


        FlowRule.Builder primaryInternetRule = DefaultFlowRule.builder();
        primaryInternetRule.withSelector(primaryInternetSelector.build());
        primaryInternetRule.withTreatment(primaryInternetTreatment.build());
        primaryInternetRule.withPriority(43001);
        primaryInternetRule.fromApp(appId);
        primaryInternetRule.forTable(ACL_TABLE);
        primaryInternetRule.makePermanent();
        primaryInternetRule.forDevice(deviceId);

        flowRuleService.applyFlowRules(primaryInternetRule.build());


        TrafficSelector.Builder secondaryInternetSelector = DefaultTrafficSelector.builder();
        secondaryInternetSelector.matchInPort(secondaryInternet);
        secondaryInternetSelector.matchVlanId(secondaryInternetVlan);

        TrafficTreatment.Builder secondaryInternetTreatment = DefaultTrafficTreatment.builder();
        secondaryInternetTreatment.group(GroupFinder.getL2Interface(secondaryQuagga, secondaryInternetVlan, deviceId));


        FlowRule.Builder secondaryInternetRule = DefaultFlowRule.builder();
        secondaryInternetRule.withSelector(secondaryInternetSelector.build());
        secondaryInternetRule.withTreatment(secondaryInternetTreatment.build());
        secondaryInternetRule.withPriority(43002);
        secondaryInternetRule.fromApp(appId);
        secondaryInternetRule.forTable(ACL_TABLE);
        secondaryInternetRule.makePermanent();
        secondaryInternetRule.forDevice(deviceId);

        flowRuleService.applyFlowRules(secondaryInternetRule.build());


        //Server to Internet

        TrafficSelector.Builder primaryQuaggaSelector = DefaultTrafficSelector.builder();
        primaryQuaggaSelector.matchInPort(primaryQuagga);
        primaryQuaggaSelector.matchVlanId(primaryInternetVlan);

        TrafficTreatment.Builder primaryQuaggaTreatment = DefaultTrafficTreatment.builder();
        primaryQuaggaTreatment.group(GroupFinder.getL2Interface(primaryInternet, primaryInternetVlan,true, deviceId));


        FlowRule.Builder primaryQuaggaRule = DefaultFlowRule.builder();
        primaryQuaggaRule.withSelector(primaryQuaggaSelector.build());
        primaryQuaggaRule.withTreatment(primaryQuaggaTreatment.build());
        primaryQuaggaRule.withPriority(41031);
        primaryQuaggaRule.fromApp(appId);
        primaryQuaggaRule.forTable(ACL_TABLE);
        primaryQuaggaRule.makePermanent();
        primaryQuaggaRule.forDevice(deviceId);

        flowRuleService.applyFlowRules(primaryQuaggaRule.build());


        TrafficSelector.Builder secondaryQuaggaSelector = DefaultTrafficSelector.builder();
        secondaryQuaggaSelector.matchInPort(secondaryQuagga);
        secondaryQuaggaSelector.matchVlanId(secondaryInternetVlan);

        TrafficTreatment.Builder secondaryQuaggaTreatment = DefaultTrafficTreatment.builder();
        secondaryQuaggaTreatment.group(GroupFinder.getL2Interface(secondaryInternet, secondaryInternetVlan,true, deviceId));


        FlowRule.Builder secondaryQuaggaRule = DefaultFlowRule.builder();
        secondaryQuaggaRule.withSelector(secondaryQuaggaSelector.build());
        secondaryQuaggaRule.withTreatment(secondaryQuaggaTreatment.build());
        secondaryQuaggaRule.withPriority(41032);
        secondaryQuaggaRule.fromApp(appId);
        secondaryQuaggaRule.forTable(ACL_TABLE);
        secondaryQuaggaRule.makePermanent();
        secondaryQuaggaRule.forDevice(deviceId);

        flowRuleService.applyFlowRules(secondaryQuaggaRule.build());

    }






}


