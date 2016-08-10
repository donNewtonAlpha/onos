package org.onosproject.noviaggswitch;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by nick on 8/8/16.
 */
public class LinkFailureDetection implements DeviceListener{

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


    @Override
    public void event(DeviceEvent event) {

        log.info(event.toString());

        PortNumber affectedPort = event.port().number();
        DeviceId affectedDevice = event.subject().id();


        for(ConnectPoint cp : redundancyPorts) {


            if(cp.port().equals(affectedPort) && cp.deviceId().equals(affectedDevice)){
                //this port is affected

                log.info(" Port " + affectedPort.toString() + " from device " + affectedDevice.toString() + " affected");

                if(event.type() == DeviceEvent.Type.PORT_REMOVED) {

                    //Port Down
                    //remove the matching flows and
                    //TODO : notify maintenance/...

                    Iterable<FlowRule> flows = flowRuleService.getFlowRulesById(NoviAggSwitchComponent.appId);
                    List<FlowRule> flowsToWithdraw = new LinkedList<>();
                    Instruction outputToDeadLink = DefaultTrafficTreatment.builder().setOutput(cp.port()).build().immediate().get(0);

                    for(FlowRule flow : flows) {
                        if(flow.deviceId().equals(cp.deviceId())) {
                            if (flow.treatment().immediate().contains(outputToDeadLink)) {
                                //flow affected
                                flowsToWithdraw.add(flow);
                                flowRuleService.removeFlowRules(flow);
                            }
                        }
                    }

                    log.warn("Port down : " + affectedPort.toString() + " on device " + affectedDevice.toString() + ", " + flowsToWithdraw.size() + " flows withdrawn. Maintenance requested");

                    withdrawnFlows.add(new WithdrawnFlows(flowsToWithdraw, event.time(), cp));

                }

                if(event.type() == DeviceEvent.Type.PORT_ADDED) {
                    //Port back up, reinstate the flows
                    for (WithdrawnFlows wFlows : withdrawnFlows) {
                        if(wFlows.getPort().equals(cp)){
                            //Previous flows are reinstanted
                            for(FlowRule flow : wFlows.getFlows()) {
                                flowRuleService.applyFlowRules(flow);
                            }
                            log.warn("Port up : " + affectedPort.toString() + " on " + affectedDevice.toString() + ", " + wFlows.getFlows().size() + " flows reinstanted.");

                            withdrawnFlows.remove(wFlows);
                        }
                    }

                }

            }
        }

    }

    private class WithdrawnFlows {

        private List<FlowRule> flows;
        private long timestamp;
        private ConnectPoint port;


        public WithdrawnFlows(List<FlowRule> flows, long timestamp,ConnectPoint port) {

            this.flows = flows;
            this.timestamp = timestamp;
            this.port = port;
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

        public boolean equals( Object other) {
            if ( other instanceof WithdrawnFlows) {

                return (timestamp == ((WithdrawnFlows) other).getTimestamp())&&(port.equals(((WithdrawnFlows) other).getPort()));

            }
            return false;
        }
    }

}
