package org.onosproject.testing;


import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.*;
import org.onosproject.net.meter.*;

import java.util.Collections;


/**
 * Created by nick on 7/5/16.
 */
public class NoviFlowTest {

    private static DeviceId deviceId;
    private static ApplicationId appId;
    private static FlowRuleService flowRuleService;
    private static MeterService meterService;

    private static PortNumber outPort = PortNumber.portNumber(8);
    private static MacAddress nextHopMac = MacAddress.valueOf("a0:36:9f:27:88:f0");


    public static void bng(DeviceId deviceId, ApplicationId appId, FlowRuleService flowRuleService, MeterService meterService) {

        NoviFlowTest.deviceId = deviceId;
        NoviFlowTest.appId = appId;
        NoviFlowTest.flowRuleService = flowRuleService;
        NoviFlowTest.meterService = meterService;



        for(int i = 2; i < 4 ; i++) {

            sTagMatch(5, i);

            for(int j = 2; j < 4 ; j++) {

                meterAssignmentAndOut(i, j, (i + j) * 1000);

            }
        }

        acl();


    }

    private static void sTagMatch(int port, int sTag){


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

    private static void aclDropFlow(Ip4Address dstIP, Ip4Address srcIp){

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

    private static void acl(){

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.transition(2);

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

    private static void meterAssignmentAndOut(int sTag, int cTag, int kbps){

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
        treatment.setEthDst(nextHopMac);
        treatment.meter(finalMeter.id());
        treatment.setOutput(outPort);

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(1);
        rule.forTable(2);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        flowRuleService.applyFlowRules(rule.build());

    }

}
