package org.onosproject.noviaggswitch;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.driver.extensions.NoviflowSetVxLan;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by nick on 8/8/16.
 */
public class LinkFailureDetection {

    private final Logger log = LoggerFactory.getLogger(getClass());


    private FlowRuleService flowRuleService;
    private List<ConnectPoint> redundancyPorts;
    private List<WithdrawnFlows> withdrawnFlows;

    public LinkFailureDetection(FlowRuleService flowRuleService, List<ConnectPoint> redundancyPorts){


        this.flowRuleService = flowRuleService;
        this.redundancyPorts = redundancyPorts;
        this.withdrawnFlows = new LinkedList<>();

    }

    public void addRedundancyPort (ConnectPoint port){
        redundancyPorts.add(port);
    }

    public void removeRedundancyPort(ConnectPoint port) {
        redundancyPorts.remove(port);
    }

    public void removeDevice(DeviceId deviceId) {

        Iterator<ConnectPoint> it = redundancyPorts.listIterator();

        while(it.hasNext()) {
            ConnectPoint cp = it.next();
            if(cp.deviceId().equals(deviceId)) {
                it.remove();
            }
        }
    }


    public void event(DeviceId affectedDevice, PortNumber affectedPort, boolean failure, boolean newMac, MacAddress oldMac, MacAddress newDstMac) {




        /*for (ConnectPoint cp : redundancyPorts) {


            if (cp.port().equals(affectedPort) && cp.deviceId().equals(affectedDevice)) {
                //this port is affected*/

                log.info(" Port " + affectedPort.toString() + " from device " + affectedDevice.toString() + " affected");



                if (failure) {

                    //Port Down
                    //remove the matching flows and
                    //TODO : notify maintenance/...

                    Iterable<FlowRule> flows = flowRuleService.getFlowRulesById(NoviAggSwitchComponent.appId);
                    List<FlowRule> flowsToWithdraw = new LinkedList<>();
                    Instruction outputToDeadLink = DefaultTrafficTreatment.builder().setOutput(affectedPort).build().immediate().get(0);

                    for (FlowRule flow : flows) {
                        if (flow.deviceId().equals(affectedDevice)) {
                            if (flow.treatment().immediate().contains(outputToDeadLink)) {

                                //Check for old mac

                                if (checkDstMac(flow, oldMac)) {
                                    //flow affected
                                    flowsToWithdraw.add(flow);
                                    flowRuleService.removeFlowRules(flow);
                                }
                            }
                        }
                    }

                    log.warn("Port down : " + affectedPort.toString() + " on device " + affectedDevice.toString() + ", " + flowsToWithdraw.size() + " flows withdrawn. Maintenance requested");

                    withdrawnFlows.add(new WithdrawnFlows(flowsToWithdraw, System.currentTimeMillis(), new ConnectPoint(affectedDevice, affectedPort), oldMac));

                } else {
                    // Port back up, reinstate the flows
                    Iterator<WithdrawnFlows> it = withdrawnFlows.listIterator();

                    if(!newMac) {
                        //Reinstate as is
                        while (it.hasNext()) {
                            WithdrawnFlows wFlows = it.next();
                            if (wFlows.getPort().equals(new ConnectPoint(affectedDevice, affectedPort)) && wFlows.getDstMac().equals(oldMac)) {
                                //Previous flows are reinstanted
                                for (FlowRule flow : wFlows.getFlows()) {
                                    flowRuleService.applyFlowRules(flow);
                                }

                                log.warn("Link back up, port : " + affectedPort.toString() + " on " + affectedDevice.toString() + ", " + wFlows.getFlows().size() + " flows reinstanted.");
                                it.remove();
                            }
                        }
                    } else {
                        //New dst mac

                        while(it.hasNext()) {
                            WithdrawnFlows wFlows = it.next();
                            if (wFlows.getPort().equals(new ConnectPoint(affectedDevice, affectedPort)) && wFlows.getDstMac().equals(oldMac)) {

                                List<FlowRule> flows = wFlows.getFlows();
                                for(FlowRule flow : flows) {
                                    //changing the dst Mac
                                    alterMacAndApply(flow, newDstMac);
                                }

                                it.remove();
                            }
                        }

                    }

                }

        /*    }
        }*/


    }

    private void alterMacAndApply(FlowRule flow, MacAddress newMac) {

        //Look for setVxlan and alter remote Mac
        List<Instruction> instructions = flow.treatment().immediate();
        for (Instruction instruction : instructions) {

            if (instruction.type() == Instruction.Type.EXTENSION) {


                try {
                    Instructions.ExtensionInstructionWrapper extensionInstruction = (Instructions.ExtensionInstructionWrapper) instruction;
                    ExtensionTreatment extension = extensionInstruction.extensionInstruction();


                    if (extension.type().equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NOVIFLOW_SET_VXLAN.type())) {
                        log.info("set vxlan extension found");

                        NoviflowSetVxLan noviflowVxlan = (NoviflowSetVxLan) extension;
                        noviflowVxlan.setDstMac(newMac);

                    }

                } catch (Exception e) {
                    log.warn("alterMacAndApply exception", e);
                }
            }
        }

        flowRuleService.applyFlowRules(flow);
    }

    private boolean checkDstMac(FlowRule flow, MacAddress expectedDstMac) {

        List<Instruction> instructions = flow.treatment().immediate();
        for (Instruction instruction : instructions) {

            if (instruction.type() == Instruction.Type.EXTENSION) {

                try {
                    Instructions.ExtensionInstructionWrapper extensionInstruction = (Instructions.ExtensionInstructionWrapper) instruction;
                    ExtensionTreatment extension = extensionInstruction.extensionInstruction();

                    if (extension.type().equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NOVIFLOW_SET_VXLAN.type())) {

                        log.debug("Extension treatment : " + extension.toString());

                        NoviflowSetVxLan noviflowVxlan = (NoviflowSetVxLan) extension;

                        return noviflowVxlan.getDstMac().equals(expectedDstMac);

                    }
                } catch (Exception e) {
                    log.error("Exception", e);
                }
            }
        }
        return false;
    }

    private class WithdrawnFlows {

        private List<FlowRule> flows;
        private long timestamp;
        private ConnectPoint port;
        private MacAddress dstMac;


        public WithdrawnFlows(List<FlowRule> flows, long timestamp,ConnectPoint port, MacAddress mac) {

            this.flows = flows;
            this.timestamp = timestamp;
            this.port = port;
            this.dstMac = mac;
        }

        public long getTimestamp(){
            return timestamp;
        }

        public List<FlowRule> getFlows() {
            return flows;
        }

        public ConnectPoint getPort() {
            return port;
        }

        public MacAddress getDstMac() {
            return dstMac;
        }

        public boolean equals( Object other) {
            if ( other instanceof WithdrawnFlows) {

                return (timestamp == ((WithdrawnFlows) other).getTimestamp())&&(port.equals(((WithdrawnFlows) other).getPort()));

            }
            return false;
        }
    }

}
