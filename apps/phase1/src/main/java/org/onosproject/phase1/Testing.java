package org.onosproject.phase1;

import org.onosproject.core.GroupId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.*;
import org.onosproject.net.group.*;

import java.nio.ByteBuffer;
import java.util.Collections;

/**
 * Created by nick on 12/8/15.
 */
public class Testing {

    private static FlowRuleService flowRuleService;
    private static GroupService groupService;

    static void initiate(FlowRuleService flowRuleService, GroupService groupService){
        Testing.flowRuleService = flowRuleService;
        Testing.groupService = groupService;
    }

    static void connectPorts(int port1, int port2){

        //Create outputGroups

        TrafficTreatment.Builder outputToPort2 = DefaultTrafficTreatment.builder();
        outputToPort2.setOutput(PortNumber.portNumber(port2));

        Integer outputToPort2GroupId =  (11 <<  28) |((short) port2);
        final GroupKey outputToPort2Groupkey = new DefaultGroupKey(ByteBuffer.allocate(4).putInt(outputToPort2GroupId).array());

        GroupBucket outputToPort2Bucket =
                DefaultGroupBucket.createIndirectGroupBucket(outputToPort2.build());
        GroupDescription outputToPort2GroupDescription = new DefaultGroupDescription(Phase1Component.torId,
                GroupDescription.Type.INDIRECT,
                new GroupBuckets(Collections.singletonList(outputToPort2Bucket)),
                outputToPort2Groupkey,
                outputToPort2GroupId,
                Phase1Component.appId);
        groupService.addGroup(outputToPort2GroupDescription);

        GroupId toPort2 = groupService.getGroup(Phase1Component.torId, outputToPort2Groupkey).id();

        TrafficTreatment.Builder outputToPort1 = DefaultTrafficTreatment.builder();
        outputToPort1.setOutput(PortNumber.portNumber(port1));

        Integer outputToPort1GroupId =  (11 <<  28) |((short) port1);
        final GroupKey outputToPort1Groupkey = new DefaultGroupKey(ByteBuffer.allocate(4).putInt(outputToPort1GroupId).array());

        GroupBucket outputToPort1Bucket =
                DefaultGroupBucket.createIndirectGroupBucket(outputToPort1.build());
        GroupDescription outputToPort1GroupDescription = new DefaultGroupDescription(Phase1Component.torId,
                GroupDescription.Type.INDIRECT,
                new GroupBuckets(Collections.singletonList(outputToPort1Bucket)),
                outputToPort1Groupkey,
                outputToPort1GroupId,
                Phase1Component.appId);
        groupService.addGroup(outputToPort1GroupDescription);

        GroupId toPort1 = groupService.getGroup(Phase1Component.torId, outputToPort1Groupkey).id();



        TrafficSelector.Builder selector1 = DefaultTrafficSelector.builder();
        selector1.matchInPort(PortNumber.portNumber(port1));

        TrafficTreatment.Builder treatment1 = DefaultTrafficTreatment.builder();
        treatment1.group(toPort2);

        FlowRule.Builder rule1 = DefaultFlowRule.builder();
        rule1.withSelector(selector1.build());
        rule1.withTreatment(treatment1.build());
        rule1.withPriority(51001);
        rule1.makePermanent();
        rule1.fromApp(Phase1Component.appId);
        rule1.forTable(60);
        rule1.forDevice(Phase1Component.torId);

        flowRuleService.applyFlowRules(rule1.build());

        TrafficSelector.Builder selector2 = DefaultTrafficSelector.builder();
        selector2.matchInPort(PortNumber.portNumber(port2));

        TrafficTreatment.Builder treatment2 = DefaultTrafficTreatment.builder();
        treatment1.group(toPort1);

        FlowRule.Builder rule2 = DefaultFlowRule.builder();
        rule1.withSelector(selector2.build());
        rule1.withTreatment(treatment2.build());
        rule1.withPriority(51002);
        rule1.makePermanent();
        rule1.fromApp(Phase1Component.appId);
        rule1.forTable(60);
        rule1.forDevice(Phase1Component.torId);

        flowRuleService.applyFlowRules(rule2.build());


    }

}
