package org.onosproject.noviaggswitch;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
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
public class LinkFailureDetection implements LinkListener{

    private final Logger log = LoggerFactory.getLogger(getClass());

    private LinkService linkService;
    private FlowRuleService flowRuleService;
    private List<ConnectPoint> redundancyPorts;
    private List<WithdrawnFlows> withdrawnFlows;

    public LinkFailureDetection(LinkService linkService, FlowRuleService flowRuleService, List<ConnectPoint> redundancyPorts){

        this.linkService = linkService;
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
    public void event(LinkEvent event) {

        log.info(event.toString());

        for(ConnectPoint cp : redundancyPorts) {

            //Get link(s) for this ports
            Set<Link> links = linkService.getLinks(cp);

            for(Link link : links) {

                if(link.equals(event.subject())){
                    //this link so this port is affected

                    log.info(" Link " + link.toString() + " from " + cp.toString() + " affected");

                    if(event.type() == LinkEvent.Type.LINK_REMOVED) {

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

                        log.warn("Link down : " + link.toString() + " on " + cp.toString() + ", " + flowsToWithdraw.size() + " flows withdrawn. Maintenance requested");

                        withdrawnFlows.add(new WithdrawnFlows(flowsToWithdraw, event.time(), cp));

                    }

                    if(event.type() == LinkEvent.Type.LINK_ADDED) {
                        //Port back up, reinstate the flows
                        for (WithdrawnFlows wFlows : withdrawnFlows) {
                            if(wFlows.getPort().equals(cp)){
                                //Previous flows are reinstanted
                                for(FlowRule flow : wFlows.getFlows()) {
                                    flowRuleService.applyFlowRules(flow);
                                }
                                log.warn("Link up : " + link.toString() + " on " + cp.toString() + ", " + wFlows.getFlows().size() + " flows reinstanted.");

                                withdrawnFlows.remove(wFlows);
                            }
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
