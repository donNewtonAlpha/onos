
package org.onosproject.testing;

import ofdpagroups.GroupFinder;
import org.apache.felix.scr.annotations.*;



import org.onlab.packet.*;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.driver.extensions.NoviflowPopVxLan;
import org.onosproject.driver.extensions.NoviflowSetVxLan;
import org.onosproject.driver.extensions.OfdpaMatchVlanVid;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.*;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.*;
import org.onosproject.net.meter.MeterService;
import org.onosproject.net.packet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;


/**
 * Created by nick on 6/26/15.
 */
@Component(immediate = true)

public class TestingComponent {


    static final Logger log = LoggerFactory.getLogger(TestingComponent.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MeterService meterService;



    static ApplicationId appId;
    static final DeviceId deviceId = DeviceId.deviceId("of:000000000000da7a");
    static final DeviceId ovsId = DeviceId.deviceId("of:0000b214876af640");
    static final DeviceId noviflowDevice = DeviceId.deviceId("of:000000223d5a00d9");


    static final int ACL_TABLE = 60;
    static final int VLAN_TABLE = 10;
    static final int TMAC_TABLE = 20;
    static final int UNICAST_ROUTING_TABLE = 30;
    static final int BRIDGING_TABLE = 50;


    @Activate
    protected void activate() {

        log.debug("trying to activate");
        appId = coreService.registerApplication("org.onosproject.testing");

        NoviFlowTest.bng(noviflowDevice, appId, flowRuleService, meterService);




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

    private void ovsTest(DeviceId ovsId) {

        TrafficSelector.Builder selector1 = DefaultTrafficSelector.builder();
        selector1.matchEthType(Ethernet.TYPE_IPV4);
        selector1.matchIPDst(IpPrefix.valueOf("192.168.1.192/26"));

        TrafficSelector.Builder selector2 = DefaultTrafficSelector.builder();
        selector2.matchEthType(Ethernet.TYPE_IPV4);
        selector2.matchIPDst(IpPrefix.valueOf("192.168.1.254/32"));

        TrafficSelector.Builder selector3 = DefaultTrafficSelector.builder();
        selector3.matchEthType(Ethernet.TYPE_ARP);
        selector3.matchArpTpa(Ip4Address.valueOf("192.168.1.199"));


        TrafficTreatment.Builder outPort1 = DefaultTrafficTreatment.builder();
        outPort1.setOutput(PortNumber.portNumber(1));

        TrafficTreatment.Builder outPort2 = DefaultTrafficTreatment.builder();
        outPort2.setOutput(PortNumber.portNumber(2));

        addFlow(selector1.build(), outPort1.build(), 1000, ovsId);
        addFlow(selector2.build(), outPort2.build(), 5000, ovsId);
        addFlow(selector3.build(), outPort1.build(), 500, ovsId);

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

    private void multiplyTraffic (List<PortNumber> inPorts, List<PortNumber> shitHookPorts, PortNumber outPort, DeviceId deviceId) {
        // Assuming shithook port n is connected to port n+1

        //Cumulus vlans : 3793
        for(int i = 2; i< 5; i++) {

            VlanId vlan = VlanId.vlanId((short)i);

            for(PortNumber port : inPorts) {
                vlanTableFlows(port, vlan, deviceId);
            }

            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {

            }

            multicastTraffic(inPorts, shitHookPorts, vlan, deviceId);
            mergeTraffic(shitHookPorts, outPort, vlan, deviceId);

        }

    }

    private void multicastTraffic(List<PortNumber> inPorts, List<PortNumber> outPorts, VlanId vlanId, DeviceId deviceId) {

        for(PortNumber in : inPorts) {

            TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
            selector.matchInPort(in);
            selector.extension(new OfdpaMatchVlanVid(vlanId), deviceId);

            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            treatment.group(GroupFinder.getL2Multicast(outPorts, vlanId, deviceId));

            FlowRule.Builder rule = DefaultFlowRule.builder();
            rule.withSelector(selector.build());
            rule.withTreatment(treatment.build());
            rule.withPriority(45000);
            rule.forTable(ACL_TABLE);
            rule.fromApp(appId);
            rule.forDevice(deviceId);
            rule.makePermanent();

            flowRuleService.applyFlowRules(rule.build());

        }

        //Vlan flows for incoming shithook

        for(PortNumber outport : outPorts) {
            PortNumber shitHookIn = PortNumber.portNumber(outport.toLong());

            vlanTableFlows(shitHookIn, vlanId, deviceId);
        }


    }

    private void mergeTraffic(List<PortNumber> toAggregatePorts, PortNumber outputPort, VlanId vlanId, DeviceId deviceId) {

        for(PortNumber shitHook : toAggregatePorts) {

            PortNumber port = PortNumber.portNumber(shitHook.toLong() + 1);


            TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
            selector.matchInPort(port);
            selector.extension(new OfdpaMatchVlanVid(vlanId), deviceId);

            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            treatment.group(GroupFinder.getL2Interface(outputPort, vlanId, deviceId));

            FlowRule.Builder rule = DefaultFlowRule.builder();
            rule.withSelector(selector.build());
            rule.withTreatment(treatment.build());
            rule.withPriority(46000);
            rule.forTable(ACL_TABLE);
            rule.fromApp(appId);
            rule.forDevice(deviceId);
            rule.makePermanent();

            flowRuleService.applyFlowRules(rule.build());
        }


    }


    private void addFlow(TrafficSelector selector, TrafficTreatment treatment, int priority, DeviceId deviceId) {

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector);
        rule.withTreatment(treatment);
        rule.withPriority(priority);
        rule.fromApp(appId);
        rule.makePermanent();
        rule.forTable(0);
        rule.forDevice(deviceId);

        flowRuleService.applyFlowRules(rule.build());
    }

    private void inbandOltControl(){

        flowPort1to2(25,1);
        flowPort1to2(26,1);
        flowPort1to2(27,1);
        flowPort1to2(28,1);

        GroupKey groupKey = new DefaultGroupKey(ByteBuffer.allocate(4).putInt(42).array());
        List<GroupBucket> buckets = new LinkedList<>();

        for(int i = 25; i < 29; i++) {

            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            treatment.setOutput(PortNumber.portNumber(i));

            GroupBucket bucket = DefaultGroupBucket.createAllGroupBucket(treatment.build());

            buckets.add(bucket);
        }

        GroupDescription groupDescription = new DefaultGroupDescription(deviceId,
                GroupDescription.Type.ALL,
                new GroupBuckets(buckets),
                groupKey,
                42,
                appId);
        groupService.addGroup(groupDescription);


        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchInPort(PortNumber.portNumber(2));

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.group(groupService.getGroup(deviceId, groupKey).id());

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(8000);
        rule.fromApp(appId);
        rule.makePermanent();
        rule.forTable(0);
        rule.forDevice(deviceId);

        flowRuleService.applyFlowRules(rule.build());


    }

    private void linkPorts(int port1, int port2) {
        flowPort1to2(port1, port2);
        flowPort1to2(port2, port1);
    }

    private void flowPort1to2(int port1, int port2){

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchInPort(PortNumber.portNumber(port1));

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.setOutput(PortNumber.portNumber(port2));

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(2000);
        rule.fromApp(appId);
        rule.makePermanent();
        rule.forTable(0);
        rule.forDevice(deviceId);

        flowRuleService.applyFlowRules(rule.build());

    }

    private void ipFlow(Ip4Address ip, int outport) {

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchIPDst(ip.toIpPrefix());

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.setOutput(PortNumber.portNumber(outport));

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(3000);
        rule.fromApp(appId);
        rule.makePermanent();
        rule.forTable(0);
        rule.forDevice(deviceId);

        flowRuleService.applyFlowRules(rule.build());
    }

    private void vxlanTest(int inPort, int outPort, VlanId matchVlan) {

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchVlanId(matchVlan);
        selector.matchInPort(PortNumber.portNumber(inPort));

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.extension(new NoviflowSetVxLan(MacAddress.valueOf("11:55:22:33:66:88"), MacAddress.valueOf("55:66:33:11:55:88"),
                Ip4Address.valueOf("172.55.1.36"), Ip4Address.valueOf("145.45.14.1"), 15, 8), deviceId);
        treatment.setOutput(PortNumber.portNumber(outPort));

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(2500);
        rule.fromApp(appId);
        rule.makePermanent();
        rule.forTable(0);
        rule.forDevice(deviceId);

        flowRuleService.applyFlowRules(rule.build());

    }

    private void vxlanFlow(int inPort, int outPort, Ip4Address dstIp, Ip4Address srcIp, MacAddress dstMac, MacAddress srcMac, int udpPort, int vni) {

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchInPort(PortNumber.portNumber(inPort));

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.extension(new NoviflowSetVxLan(srcMac, dstMac, srcIp, dstIp, udpPort, vni), deviceId);
        treatment.setOutput(PortNumber.portNumber(outPort));

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(3000);
        rule.fromApp(appId);
        rule.makePermanent();
        rule.forTable(0);
        rule.forDevice(deviceId);

        flowRuleService.applyFlowRules(rule.build());

    }

    private void popVxLanFlow(int inPort, int outPort) {

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchInPort(PortNumber.portNumber(inPort));

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.extension(new NoviflowPopVxLan(), deviceId);
        treatment.setOutput(PortNumber.portNumber(outPort));

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(3500);
        rule.fromApp(appId);
        rule.makePermanent();
        rule.forTable(0);
        rule.forDevice(deviceId);

        flowRuleService.applyFlowRules(rule.build());

    }

    private void vlanCrossconnect(int port1, int port2, int vlanId) {

        vlanOneWayCrossconnect(port1, port2, vlanId);
        vlanOneWayCrossconnect(port2, port1, vlanId);

    }

    private void vlanOneWayCrossconnect(int inPort, int outPort, int vlanId) {

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchInPort(PortNumber.portNumber(inPort));
        selector.matchVlanId(VlanId.vlanId((short) vlanId));

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.setOutput(PortNumber.portNumber(outPort));

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(7000);
        rule.fromApp(appId);
        rule.makePermanent();
        rule.forTable(0);
        rule.forDevice(deviceId);

        flowRuleService.applyFlowRules(rule.build());
    }

 /*   private FlowRule testFlow1(int inPort, int outPort, int vlan){
        return testFlow1(inPort, outPort, vlan, null);
    }

    private FlowRule testFlow1(int inPort, int outPort, int vlan, MacAddress dstMacToMatch) {

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchInPort(PortNumber.portNumber(inPort));
        if(vlan >=0) {
            selector.matchVlanId(VlanId.vlanId((short) vlan));
        } else {
            selector.matchVlanId(VlanId.NONE);
        }
        if(dstMacToMatch != null){
            selector.matchEthDst(dstMacToMatch);
        }

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.setOutput(PortNumber.portNumber(outPort));

        FlowRule.Builder flow = DefaultFlowRule.builder();
        flow.withSelector(selector.build());
        flow.withTreatment(treatment.build());
        flow.withPriority(45678);
        flow.forDevice(torId);
        flow.fromApp(appId);
        flow.makePermanent();

        return flow.build();
    }


    private void testFlowTranslation(FlowRule ruleToTranslate){

        FlowRule[] translatedRules = FlowRuleTranslation.translate(ruleToTranslate);

        for (int i = 0; i < translatedRules.length; i++) {
            log.info("Rule " + i + " : " + translatedRules[i].toString());
        }

        flowRuleService.applyFlowRules(translatedRules);

    }

    private void testMultipleOutputTreatment() {

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        for (int i = 3; i < 9; i++){
            treatment.setOutput(PortNumber.portNumber(i));
        }

        log.info("Treatment : " + treatment.build().toString());


    }

    private void testRemoveFlowTranslation(FlowRule flowToTranslateAndRemove){

        FlowRule[] translatedRules = FlowRuleTranslation.translate(flowToTranslateAndRemove);

        for (int i = 0; i < translatedRules.length; i++) {
            log.info("Rule " + i + " : " + translatedRules[i].toString());
        }

        flowRuleService.removeFlowRules(translatedRules);
    }*/



}


