package org.onosproject.translation;


import org.apache.felix.scr.annotations.*;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.translation.ofdpa2Translation.Ofdpa2Translation;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by nick on 2/15/16.
 */

@Component(immediate = true)
public class FlowRuleTranslation {

    static final Logger log = LoggerFactory.getLogger(FlowRuleTranslation.class);

    private static final int MATCH_KEY_FACTOR = 10000;


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService1;

    private static DeviceService deviceService;


    static ApplicationId appId;



    @Activate
    protected void activate() {

        log.debug("trying to activate");
        appId = coreService.registerApplication("org.onosproject.translation");

        FlowRuleTranslation.deviceService = deviceService1;

        try {
            Ofdpa2Translation.init(groupService, coreService);
        } catch ( Exception e){
            log.error("failed to initiate Ofdpa2Translation", e);
        }



    }




    @Deactivate
    protected void deactivate() {
    }




    public static FlowRule[] translate(FlowRule originalRule){

        int key = getFlowTypeKey(originalRule);
        return breakDownFlowRule(originalRule, key);

    }

    private static int getFlowTypeKey(FlowRule flowRule){

        int matchKey = 0;
        int actionKey = 0;

        List<Criterion> criterionList = new LinkedList<>(flowRule.selector().criteria());
        List<Instruction> instructionList = new LinkedList<>(flowRule.treatment().allInstructions());

        //Determine matchKey
        matchKey = determineMatchKey(criterionList);
        actionKey = determineActionKey(instructionList);


        if(matchKey*actionKey == 0){
            log.error("FlowType not implemented for any switch");
        }

        return matchKey*MATCH_KEY_FACTOR + actionKey;
    }

    private static int determineMatchKey(List<Criterion> criterionList){

        if(contains(criterionList, Criterion.Type.IN_PORT)){
            if(contains(criterionList, Criterion.Type.VLAN_VID)){
                if(criterionList.isEmpty()){
                    log.debug("mactchkey 1");
                    return 1;
                }
                if(contains(criterionList, Criterion.Type.ETH_DST)){
                    log.debug("matchKey 2");
                    return 2;
                }
            }


        } else {

        }



        return 0;
    }

    private static int determineActionKey(List<Instruction> instructionList){

        //L2 modification
        List<L2ModificationInstruction> l2ModificationInstructions = filterL2ModificationInstructions(instructionList);

        if(contains(instructionList, Instruction.Type.OUTPUT)) {
            if(instructionList.isEmpty()){
                log.debug("actionKey 1");
                return 1;
            }
            //TODO: other cases

        }

        return 0;
    }

    private static FlowRule[] breakDownFlowRule(FlowRule flowRule, int flowTypeKey){

        Device device = deviceService.getDevice(flowRule.deviceId());

        //TODO : device.hwVersion ?
        switch (device.hwVersion()){
            case "OF-DPA 2.0":
                return Ofdpa2Translation.translateToOfdpa(flowRule, flowTypeKey);
            default:
                log.error("Chipset unknown");
                return new FlowRule[0];

        }


    }

    private static boolean contains(List<Criterion> criterions, Criterion.Type criterionType){
        for(Criterion criterion : criterions){
            if(criterion.type().equals(criterionType)){
                criterions.remove(criterion);
                return true;
            }
        }
        return false;
    }

    private static List<L2ModificationInstruction> filterL2ModificationInstructions(List<Instruction> instructions){

        List<L2ModificationInstruction> filteredInstructions = new LinkedList<>();

        for(Instruction instruction: instructions){
            if(instruction.type().equals(Instruction.Type.L2MODIFICATION)){
                filteredInstructions.add((L2ModificationInstruction)instruction);
            }
        }

        return filteredInstructions;
    }

    private static boolean contains(List<Instruction> instructions, Instruction.Type type){
        for(Instruction instruction : instructions){
            if(instruction.type().equals(type)){
                instructions.remove(instruction);
                return true;
            }
        }
        return false;
    }

    private static boolean containsSubType(List<Instruction> instructions, List<L2ModificationInstruction> l2ModificationInstructions, L2ModificationInstruction.L2SubType subType){
        for(L2ModificationInstruction currentInstruction : l2ModificationInstructions){
            if(currentInstruction.subtype().equals(subType)){
                l2ModificationInstructions.remove(currentInstruction);
                instructions.remove(currentInstruction);
                return true;
            }
        }
        return false;
    }

    //TODO: all other types and subtypes

}
