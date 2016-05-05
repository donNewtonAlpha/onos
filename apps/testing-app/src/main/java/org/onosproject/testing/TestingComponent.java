
package org.onosproject.testing;

import org.apache.felix.scr.annotations.*;



import org.onlab.packet.*;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.driver.extensions.NoviflowPopVxLan;
import org.onosproject.driver.extensions.NoviflowSetVxLan;
import org.onosproject.translation.FlowRuleTranslation;
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



    static ApplicationId appId;
    static final DeviceId deviceId = DeviceId.deviceId("of:000000223d5a00d9");
    static final DeviceId ovsId = DeviceId.deviceId("of:0000b214876af640");


    @Activate
    protected void activate() {

        log.debug("trying to activate");
        appId = coreService.registerApplication("org.onosproject.testing");


        //linkPorts(1, 2);
        //ipFlow(Ip4Address.valueOf("192.168.1.4"), 3);
        //vxlanTest(1, 2, VlanId.vlanId((short)5));

        Ip4Address vxlanSrc = Ip4Address.valueOf("10.21.12.13");
        Ip4Address vxlanDst = Ip4Address.valueOf("10.55.12.13");
        MacAddress vxlanMacDst = MacAddress.valueOf("11:11:11:11:11:11");
        MacAddress vxlanMacSrc = MacAddress.valueOf("22:22:22:22:22:22");


        vxlanFlow(2, 4,vxlanDst, vxlanSrc, vxlanMacDst, vxlanMacSrc,15,8);
        popVxLanFlow(4,2);

        vlanCrossconnect(2,4,5);


        inbandOltControl();

        ovsTest(ovsId);


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


