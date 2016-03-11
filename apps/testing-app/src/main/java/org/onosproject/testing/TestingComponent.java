
package org.onosproject.testing;

import org.apache.felix.scr.annotations.*;



import org.onlab.packet.*;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
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
    static final DeviceId torId = DeviceId.deviceId("of:000000000000da7a");


    @Activate
    protected void activate() {

        log.debug("trying to activate");
        appId = coreService.registerApplication("org.onosproject.testing");

        log.info("FlowRuleTranslation initiated");

        /*FlowRule flow1 = testFlow1(15,16,436);
        FlowRule flow2 = testFlow1(17,18,-1);

        try {
            //Test 1, should add 1 flow in the vlan table, one flow in the acl table, one group (L2 interface)
            testFlowTranslation(flow1);
        } catch (Exception e){
            log.error("Test 1 error",e);
        }
        log.info("Test 1 ended");

        try {
            //Test 2, can a treatment have multiple Output instructions ??
            testMultipleOutputTreatment();
        } catch(Exception e){
            log.error("Test 2 error", e);
        }

        log.info("Test 2 ended");

        try {
            //Test3, can a translation be use to remove the flows
            //testRemoveFlowTranslation1(flow1);
            testFlowTranslation(flow2);
        } catch (Exception e){
            log.error("Test 3 error", e);
        }

        log.info("Test 3 ended");*/

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

    private void linkPorts(int port1, int port2){


    }

    private FlowRule testFlow1(int inPort, int outPort, int vlan){
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
    }



}


