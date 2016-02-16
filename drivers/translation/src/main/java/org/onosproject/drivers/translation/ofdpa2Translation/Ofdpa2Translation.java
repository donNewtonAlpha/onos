package org.onosproject.drivers.translation.ofdpa2Translation;

import org.onosproject.drivers.translation.ofdpa2Translation.ofdpa2Groups.GroupFinder;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.driver.extensions.OfdpaMatchVlanVid;
import org.onosproject.driver.extensions.OfdpaSetVlanVid;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.*;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.group.GroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by nick on 2/15/16.
 */
public class Ofdpa2Translation {

    static final Logger log = LoggerFactory.getLogger(Ofdpa2Translation.class);

    private static CoreService coreService;

    static final int ACL_TABLE = 60;
    static final int PORT_TABLE = 0;
    static final int VLAN_TABLE = 10;
    static final int TMAC_TABLE = 20;
    static final int VLAN1_TABLE = 11;
    static final int BRIDGING_TABLE = 50;

    static final VlanId tunnelVlanId = VlanId.vlanId((short)3210);

    public static void init(GroupService groupService, CoreService coreService){
        Ofdpa2Translation.coreService = coreService;
        GroupFinder.initiate(groupService);

    }

    public static FlowRule[] translateToOfdpa(FlowRule originalRule, int flowTypeKey){



        switch (flowTypeKey){
            case 10001 :
                VlanIdCriterion vlanIdCriterion = (VlanIdCriterion) originalRule.selector().getCriterion(Criterion.Type.VLAN_VID);
                int i = 0;
                FlowRule[] rules;
                PortNumber inPort = ((PortCriterion) originalRule.selector().getCriterion(Criterion.Type.IN_PORT)).port();
                PortNumber outPort = null;
                for(Instruction instruction : originalRule.treatment().allInstructions()){
                    if(instruction instanceof Instructions.OutputInstruction){
                        outPort = ((Instructions.OutputInstruction) instruction).port();
                    }
                }

                if(outPort == null){
                    log.error("Flow type : " + flowTypeKey + ", outputPort undefined");

                }

                if(vlanIdCriterion.vlanId().equals(VlanId.NONE)){
                    rules = new FlowRule[3];
                    FlowRule[] vlanTableRules = untaggedPacketsTagging(inPort, tunnelVlanId, originalRule);
                    for(int j = 0; j< vlanTableRules.length; j++){
                        rules[j] = vlanTableRules[j];
                        i++;
                    }
                }else{
                    rules = new FlowRule[2];
                    rules[0] = vlanTableFlows(inPort, vlanIdCriterion.vlanId(), originalRule);
                    i++;
                }

                TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
                selector.matchInPort(inPort);
                selector.extension(new OfdpaMatchVlanVid(vlanIdCriterion.vlanId()), originalRule.deviceId());

                TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
                if(vlanIdCriterion.vlanId().equals(VlanId.NONE)){
                    treatment.group(GroupFinder.getL2Interface(outPort, tunnelVlanId, true, originalRule.deviceId(),coreService.getAppId(originalRule.appId())));
                } else {
                    treatment.group(GroupFinder.getL2Interface(outPort, vlanIdCriterion.vlanId(), false, originalRule.deviceId(), coreService.getAppId(originalRule.appId())));
                }

                FlowRule.Builder rule = DefaultFlowRule.builder();
                rule.withSelector(selector.build());
                rule.withTreatment(treatment.build());
                rule.withPriority(originalRule.priority());
                rule.fromApp(coreService.getAppId(originalRule.appId()));
                rule.forTable(ACL_TABLE);
                if(originalRule.isPermanent()) {
                    rule.makePermanent();
                } else {
                    rule.makeTemporary(originalRule.timeout());
                }
                rule.forDevice(originalRule.deviceId());

                rules[i] = rule.build();

                return rules;
            default:
                log.error("FlowType not implemented (maybe even not supported) for OFDPA 2");


        }


        return new FlowRule[0];
    }

    private static FlowRule vlanTableFlows(PortNumber port, VlanId vlanId, FlowRule originalRule){
        return vlanTableFlows(port, vlanId, originalRule, originalRule.priority());
    }

    private static FlowRule vlanTableFlows(PortNumber port, VlanId vlanId, FlowRule originalRule, int priority){

        TrafficSelector.Builder vlanTableSelector = DefaultTrafficSelector.builder();
        vlanTableSelector.matchInPort(port);
        vlanTableSelector.extension(new OfdpaMatchVlanVid(vlanId), originalRule.deviceId());

        TrafficTreatment.Builder vlanTableTreatment = DefaultTrafficTreatment.builder();
        vlanTableTreatment.transition(TMAC_TABLE);

        FlowRule.Builder vlanTableRule = DefaultFlowRule.builder();
        vlanTableRule.withSelector(vlanTableSelector.build());
        vlanTableRule.withTreatment(vlanTableTreatment.build());
        vlanTableRule.withPriority(priority);
        vlanTableRule.forTable(VLAN_TABLE);
        vlanTableRule.fromApp(coreService.getAppId(originalRule.appId()));
        vlanTableRule.forDevice(originalRule.deviceId());
        if(originalRule.isPermanent()){
            vlanTableRule.makePermanent();
        } else {
            vlanTableRule.makeTemporary(originalRule.timeout());
        }

        return vlanTableRule.build();

    }

    private static FlowRule[] untaggedPacketsTagging(PortNumber port, VlanId vlanId, FlowRule originalRule){

        FlowRule[] rules = new FlowRule[2];
        rules[0] = vlanTableFlows(port, vlanId,originalRule, 8);

        TrafficSelector.Builder taggingSelector = DefaultTrafficSelector.builder();
        taggingSelector.matchInPort(port);
        taggingSelector.extension(new OfdpaMatchVlanVid(VlanId.NONE), originalRule.deviceId());

        TrafficTreatment.Builder taggingTreatment = DefaultTrafficTreatment.builder();
        taggingTreatment.extension(new OfdpaSetVlanVid(vlanId), originalRule.deviceId());
        taggingTreatment.transition(TMAC_TABLE);

        FlowRule.Builder taggingRule = DefaultFlowRule.builder();
        taggingRule.withSelector(taggingSelector.build());
        taggingRule.withTreatment(taggingTreatment.build());
        if(originalRule.isPermanent()) {
            taggingRule.makePermanent();
        } else {
            taggingRule.makeTemporary(originalRule.timeout());
        }
        taggingRule.withPriority(originalRule.priority());
        taggingRule.fromApp(coreService.getAppId(originalRule.appId()));
        taggingRule.forDevice(originalRule.deviceId());
        taggingRule.forTable(VLAN_TABLE);

        rules[1] = taggingRule.build();

        return rules;

    }
}
