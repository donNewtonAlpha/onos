
package org.onosproject.tor;

import org.apache.felix.scr.annotations.*;



import org.onlab.packet.*;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.GroupId;
import org.onosproject.driver.extensions.OfdpaMatchVlanVid;
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
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
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
public class TorComponent {


    static final Logger log = LoggerFactory.getLogger(TorComponent.class);

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



    // Physical port 1, port splitting
    static final PortNumber serverPort = PortNumber.portNumber(1);
    static final PortNumber uversePort = PortNumber.portNumber(2);
    static final PortNumber internetPort = PortNumber.portNumber(2);
    // Physical port 2
    static final PortNumber oltPort = PortNumber.portNumber(3);


    static GroupId internetGroup;
    static GroupId serverWanGroup;
    static GroupId serverUverseGroup;
    static GroupId serverLanGroup;


    static GroupId oltGroup;
    static GroupId floodGroup;
    static GroupId uverseGroup;


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    static final VlanId outerTag = VlanId.vlanId((short) 5);



    @Activate
    protected void activate() {

        log.debug("trying to activate");
        appId = coreService.registerApplication("org.onosproject.tor");


        initiateOutputGroups();


        //Olt to vSGs server two way flow
        oltToServerBidirectionnal(outerTag);



    }






    private void oltToServerBidirectionnal(VlanId vlanId){

        vlanTableFlows(oltPort, vlanId);
        vlanTableFlows(serverPort, vlanId);

        ///  flowRuleService way
        OfdpaMatchVlanVid matchVlanVid = new OfdpaMatchVlanVid(vlanId);

        TrafficSelector.Builder OltToServerSelector = DefaultTrafficSelector.builder();
        OltToServerSelector.matchInPort(oltPort);
        OltToServerSelector.extension(matchVlanVid, torId);

        //ACL Table flow for olt to  server

        TrafficTreatment.Builder outputServerLan = DefaultTrafficTreatment.builder();
        outputServerLan.group(serverLanGroup);

        FlowRule.Builder oltToServer = DefaultFlowRule.builder();
        oltToServer.withSelector(OltToServerSelector.build());
        oltToServer.withTreatment(outputServerLan.build());
        oltToServer.withPriority(41003);
        oltToServer.fromApp(appId);
        oltToServer.forTable(ACL_TABLE);
        oltToServer.makePermanent();
        oltToServer.forDevice(torId);

        flowRuleService.applyFlowRules(oltToServer.build());
        ///

        ///
        TrafficSelector.Builder toOltSelector = DefaultTrafficSelector.builder();
        toOltSelector.matchInPort(serverPort);
        toOltSelector.extension(matchVlanVid, torId);

        TrafficTreatment.Builder toOltGroupTreatment = DefaultTrafficTreatment.builder();
        toOltGroupTreatment.group(oltGroup);

        FlowRule.Builder serverToOlt = DefaultFlowRule.builder();
        serverToOlt.withSelector(toOltSelector.build());
        serverToOlt.withTreatment(toOltGroupTreatment.build());
        serverToOlt.withPriority(41002);
        serverToOlt.fromApp(appId);
        serverToOlt.forTable(ACL_TABLE);
        serverToOlt.makePermanent();
        serverToOlt.forDevice(torId);

        flowRuleService.applyFlowRules(serverToOlt.build());
        ///


        /////
        TrafficSelector.Builder testSelector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_VLAN)
                .matchInPort(PortNumber.portNumber(20))
                .matchVlanId(VlanId.vlanId((short) 100));

        TrafficTreatment.Builder testTreatment = DefaultTrafficTreatment.builder()
//                .group(serverLanGroup)
                .setOutput(PortNumber.portNumber(1));

        // actually write the openflow rule
        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .fromApp(appId)
                .withSelector(testSelector.build())
                .withTreatment(testTreatment.build())
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(8888)
                .makePermanent()
                .add();

        // commit the flow rule to the switch
        flowObjectiveService.forward(torId, forwardingObjective);
        log.info("Flow rule for dest {} sent to device id {}", forwardingObjective.toString());
        /////


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

    private void vlanTableFlows(PortNumber port, VlanId vlanId){

        OfdpaMatchVlanVid matchVlanVid = new OfdpaMatchVlanVid(vlanId);

        TrafficSelector.Builder vlanTableSelector = DefaultTrafficSelector.builder();
        vlanTableSelector.matchInPort(port);
        vlanTableSelector.extension(matchVlanVid, torId);

        TrafficTreatment.Builder vlanTableTreatment = DefaultTrafficTreatment.builder();
        vlanTableTreatment.transition(TMAC_TABLE);

        FlowRule.Builder vlanTableRule = DefaultFlowRule.builder();
        vlanTableRule.withSelector(vlanTableSelector.build());
        vlanTableRule.withTreatment(vlanTableTreatment.build());
        vlanTableRule.withPriority(8);
        vlanTableRule.forTable(VLAN_TABLE);
        vlanTableRule.fromApp(appId);
        vlanTableRule.forDevice(torId);
        vlanTableRule.makePermanent();

        flowRuleService.applyFlowRules(vlanTableRule.build());

    }


    private void initiateOutputGroups(){

        TrafficTreatment.Builder outputToServerLan = DefaultTrafficTreatment.builder();
        outputToServerLan.setOutput(serverPort);

        Integer outputToServerLanGroupId =  ((outerTag.toShort()) << 16) | ((short) serverPort.toLong());
        final GroupKey outputToServerLanGroupkey = new DefaultGroupKey(ByteBuffer.allocate(4).putInt(outputToServerLanGroupId).array());

        GroupBucket outputToServerLanBucket =
                DefaultGroupBucket.createIndirectGroupBucket(outputToServerLan.build());
        GroupDescription outputToServerLanGroupDescription = new DefaultGroupDescription(torId,
                GroupDescription.Type.INDIRECT,
                new GroupBuckets(Collections.singletonList(outputToServerLanBucket)),
                outputToServerLanGroupkey,
                outputToServerLanGroupId,
                appId);
        groupService.addGroup(outputToServerLanGroupDescription);

        serverLanGroup = groupService.getGroup(torId, outputToServerLanGroupkey).id();



        TrafficTreatment.Builder toOltTreatment = DefaultTrafficTreatment.builder();
        toOltTreatment.setOutput(oltPort);


        Integer toOltGroupId =  (((outerTag.toShort()) << 16) | ((short) oltPort.toLong()));
        final GroupKey toOltGroupkey = new DefaultGroupKey(ByteBuffer.allocate(4).putInt(toOltGroupId).array());

        GroupBucket toOltBucket =
                DefaultGroupBucket.createIndirectGroupBucket(toOltTreatment.build());
        GroupDescription toOltGroupDescription = new DefaultGroupDescription(torId,
                GroupDescription.Type.INDIRECT,
                new GroupBuckets(Collections.singletonList(toOltBucket)),
                toOltGroupkey,
                toOltGroupId,
                appId);
        groupService.addGroup(toOltGroupDescription);

        oltGroup = groupService.getGroup(torId, toOltGroupkey).id();




    }



}
