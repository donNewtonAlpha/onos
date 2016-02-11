/*
 * Copyright 2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.oneping;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.*;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.onosproject.net.Host;
import org.onosproject.net.PortNumber;
import org.onosproject.net.HostId;


@Component(immediate = true)
public class Pinger {

    private static Logger log = LoggerFactory.getLogger(Pinger.class);

    private static final int PRIORITY = 10;
    private static final int FLOW_TIMEOUT = 30;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;



    private ApplicationId appId;
    private final PacketProcessor packetProcessor = new PingPacketProcessor();
    private final FlowRuleListener flowListener = new InternalFlowListener();



    // Selector for ICMP traffic that is to be intercepted
    private final TrafficSelector intercept = DefaultTrafficSelector.builder()
            .matchEthType(Ethernet.TYPE_IPV4).matchIPProtocol(IPv4.PROTOCOL_ICMP)
            .build();



    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.pinger", () -> log.info("Periscope down."));
        packetService.addProcessor(packetProcessor, PRIORITY);
        flowRuleService.addListener(flowListener);
        packetService.requestPackets(intercept, PacketPriority.REACTIVE, appId);
        log.info("Started");
    }



    @Deactivate
    public void deactivate() {
        packetService.removeProcessor(packetProcessor);
        flowRuleService.removeFlowRulesById(appId);
        flowRuleService.removeListener(flowListener);
        log.info("Stopped");
    }



    // Processes the specified ICMP ping packet, adds a flow rule based on it
    private void processPing(PacketContext context) {

        // figure out the device id.
        DeviceId deviceId = context.inPacket().receivedFrom().deviceId();
        log.info("DeviceId toString {}", deviceId.toString());

        // src and dest mac from the parsed eth packet
        Ethernet eth = context.inPacket().parsed();
        MacAddress src = eth.getSourceMAC();
        MacAddress dst = eth.getDestinationMAC();

        log.info("Ethernet toString {}", eth.toString());
        log.info("Src mac {} Dst mac {}", src, dst);

        // what we are going to match on, ethernet destination
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchEthDst(dst);

        // lookup the host and its port from the host discovery service
        HostId hostid = HostId.hostId(dst);
        Host hostdst = hostService.getHost(hostid);
        PortNumber portNumber = hostdst.location().port();
        log.info("HostId toString {}", hostid.toString());
        log.info("Host toString {}", hostdst.toString());
        log.info("PortNumber toString {}", portNumber.toString());

        // set the traffic to be set to the output port discovered
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(portNumber)
                .build();
        log.info("TrafficTreatment toString {}", treatment.toString());


        // actually write the openflow rule
        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .fromApp(appId)
                .withSelector(selectorBuilder.build())
                .withTreatment(treatment)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(PRIORITY)
                .makeTemporary(FLOW_TIMEOUT)
                .add();

        // commit the flow rule to the switch
        flowObjectiveService.forward(deviceId, forwardingObjective);
        log.info("Flow rule for dest {} sent to device id {}", hostdst.mac(), deviceId.toString());
    }



    // Indicates whether the specified packet corresponds to ICMP ping.
    private boolean isIcmpPing(Ethernet eth) {
        return eth.getEtherType() == Ethernet.TYPE_IPV4 &&
                ((IPv4) eth.getPayload()).getProtocol() == IPv4.PROTOCOL_ICMP;
    }



    // Intercepts packets
    private class PingPacketProcessor implements PacketProcessor {
        @Override
        public void process(PacketContext context) {
            Ethernet eth = context.inPacket().parsed();
            log.info("Gonna process me a packet! {}", eth.toString());
            if (isIcmpPing(eth)) {
                log.info("Its a ping packet");
                processPing(context);
            }
        }
    }



    // Listens for our flows.
    private class InternalFlowListener implements FlowRuleListener {
        @Override
        public void event(FlowRuleEvent event) {
            FlowRule flowRule = event.subject();

            if (event.type().equals(FlowRuleEvent.Type.RULE_ADD_REQUESTED)) {
                DefaultFlowRule fl = (DefaultFlowRule) flowRule;
                log.info("Flow Listen:  Type {} -- App {} -- selector {} -- treatment {} -- state {}",
                        event.type(), fl.appId(), fl.selector(), fl.treatment(), "REQUEST");
            }
            else {
                DefaultFlowEntry fl = (DefaultFlowEntry) flowRule;
                log.info("Flow Listen:  Type {} -- App {} -- selector {} -- treatment {} -- state {}",
                        event.type(), fl.appId(), fl.selector(), fl.treatment(), fl.state());
            }
        }
    }
}
