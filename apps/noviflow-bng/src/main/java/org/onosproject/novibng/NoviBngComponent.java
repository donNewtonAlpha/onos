/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.novibng;


import org.apache.felix.scr.annotations.*;



import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.IPProtocolCriterion;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterRequest;
import org.onosproject.net.meter.DefaultMeterRequest;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.DefaultBand;
import org.onosproject.net.meter.MeterService;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.onosproject.novibng.config.BngDeviceConfig;

import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Collections;
import java.util.Collection;
import java.util.Set;


/**
 * Created by nick on 6/26/15.
 */
@Component(immediate = true)

public class NoviBngComponent {


    static final Logger log = LoggerFactory.getLogger(NoviBngComponent.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MeterService meterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;



    static ApplicationId appId;


    private static final int ARP_INTERCEPT_PRIORITY = 15000;
    private static final int ICMP_INTERCEPT_PRIORITY = 14000;
    private static final int IGMP_INTERCEPT_PRIORITY = 13000;
    private static final int SUB_INTERCEPT_PRIORITY = 5000;
    private static final int UPSTREAM_SPLIT = 2500;
    private static final int DOWNSTREAM_SPLIT = 2500;
    private static final int SUBSCRIBER_DOWNSTREAM_EXIT_PRIORITY = 1600;
    private static final int SUBSCRIBER_DOWNSTREAM_TRANSITION_PRIORITY = 1400;
    private static final int SUBSCRIBER_UPSTREAM_EXIT_PRIORITY = 1300;
    private static final int SUBSCRIBER_UPSTREAM_TRANSITION_PRIORITY = 1100;



    private NoviBngPacketProcessor processor;
    //private LinkFailureDetection linkFailureDetection;

    //private HashMap<DeviceId, MulticastHandler> multicastHandlers;
    private HashMap<DeviceId, BngDeviceConfig> devicesConfig;

    public HashMap<DeviceId, HashMap<Ip4Address, SubscriberInfo>> subscribersInfo;
    private HashMap<DeviceId, List<TablesInfo>> tablesInfos;
    private HashMap<DeviceId, List<GatewayInfo>> gatewayInfos;

    private static NoviBngComponent instance = null;

    public static NoviBngComponent getComponent() {
        return instance;

    }

    //TODO : QinQ

    @Activate
    protected void activate() {

        log.debug("trying to activate");

        instance = this;
        appId = coreService.registerApplication("org.onosproject.novibng");



        //Multicast
        //multicastHandlers = new HashMap<>();

        //Configs
        devicesConfig = new HashMap<>();

        //Packet processor
        processor = new NoviBngPacketProcessor(packetService);
        packetService.addProcessor(processor, 1);
        processor.startARPingThread();

        //linkFailureDetection = new LinkFailureDetection(flowRuleService, new LinkedList<>());

        //L2 infos
        subscribersInfo = new HashMap<>();
        
        //Tables infos
        tablesInfos = new HashMap<>();

        //Gateway infos
        gatewayInfos = new HashMap<>();

        log.info("NoviFlow BNG activated");

    }


    @Deactivate
    protected void deactivate() {

        packetService.removeProcessor(processor);
        processor.stopARPingThread();


        flowRuleService.removeFlowRulesById(appId);

        try {

            Set<DeviceId> deviceIds = devicesConfig.keySet();

            for (DeviceId deviceId : deviceIds) {

                Iterable<Group> appGroups = groupService.getGroups(deviceId, appId);
                for (Group group : appGroups) {
                    groupService.removeGroup(group.deviceId(), group.appCookie(), group.appId());
                }

                //Clear meters
                Collection<Meter> meters = meterService.getMeters(deviceId);
                for (Meter meter : meters) {
                    if (meter.appId().equals(appId)) {
                        meterService.withdraw(DefaultMeterRequest.builder().remove(), meter.id());
                    }
                }
            }


        } catch (Exception e) {
            log.error("Deactivation exception", e);
        }

        log.info("Stopped");
    }

   /* private Ip4Address tagsToIpMatching(SubscriberInfo info) {

        return Ip4Address.valueOf("20.20." + sTag + "." + cTag);
    }*/


    public void allocateIpBlock(DeviceId deviceId, Ip4Prefix ipBlock, Ip4Address gatewayIp, MacAddress gatewayMac) {


        if (ipBlock.prefixLength() < 24) {
            //split the block
            int ip = ipBlock.address().toInt();
            Ip4Prefix subBlock1 = Ip4Prefix.valueOf(Ip4Address.valueOf(ip), ipBlock.prefixLength() + 1);
            Ip4Prefix subBlock2 = Ip4Prefix.valueOf(Ip4Address.valueOf(ip
                    + (int)Math.pow(2, 32 - ipBlock.prefixLength())), ipBlock.prefixLength() + 1);

            allocateIpBlock(deviceId, subBlock1, gatewayIp, gatewayMac);
            allocateIpBlock(deviceId, subBlock2, gatewayIp, gatewayMac);
            return;
        }


        //Configure gateway
        List<GatewayInfo> deviceGateways = gatewayInfos.get(deviceId);
        boolean addNewGateway = true;
        for (GatewayInfo info : deviceGateways) {

            if (info.getGatewayIp().equals(gatewayIp)) {
                addNewGateway = false;
                info.addIpBlock(ipBlock);
            }

        }
        if (addNewGateway) {
            //Add the gateway to the list of gateway needing to respond, and track which gateway for which block
            GatewayInfo newGatewayInfo = new GatewayInfo(gatewayIp, gatewayMac);
            newGatewayInfo.addIpBlock(ipBlock);
            gatewayInfos.get(deviceId).add(newGatewayInfo);
            //add the flows (ARP, PING) for the gateway
            arpIntercept(gatewayIp, deviceId);
            icmpIntercept(gatewayIp, deviceId);
            processor.addRoutingInfo(deviceId, PortNumber.ANY, ipBlock, gatewayIp, gatewayMac);
            //TODO: Not optimal, there may be problem with processor.getMac()

        }


        int tableMax = 0;
        boolean blockAssigned = false;

        for (TablesInfo tablesInfo : tablesInfos.get(deviceId)) {
            if (tablesInfo.containsBlock(ipBlock)) {
                blockAssigned = true;
            }
        }

        for (TablesInfo tableInfo : tablesInfos.get(deviceId)) {
            if (!blockAssigned) {

                if (tableInfo.tryAddIpBlock(ipBlock)) {
                    //Block assigned
                    blockAssigned = true;
                    tableSplit(tableInfo, ipBlock, deviceId);
                } else {
                    if (tableInfo.getRootTable() > tableMax) {
                        tableMax = tableInfo.getRootTable();
                    }
                }
            }
        }

        if (!blockAssigned) {
            //All tables are full, need a new table set
            TablesInfo newTable = new TablesInfo(tableMax + TablesInfo.CONSECUTIVES_TABLES);
            if (!newTable.tryAddIpBlock(ipBlock)) {
                log.error("Unable to add ip block in new tables !!!!!");
                return;
            }
            tablesInfos.get(deviceId).add(newTable);
            tableSplit(newTable, ipBlock, deviceId);

        }
        
    }
    
    public void addSubscriber(Ip4Address subscriberIp, Ip4Address gatewayIp, int uploadSpeed, int downloadSpeed,
                              DeviceId deviceId) {

        //Check if the subscriber is already configured on this device
        if (subscribersInfo.get(deviceId).containsKey(subscriberIp)) {
            log.info("Subscriber " + subscriberIp + " already configured on device " + deviceId);
            //Send to modifySubscriber instead
            modifySubscriber(subscriberIp, gatewayIp, uploadSpeed, downloadSpeed);

        }

        //Check if gateway and ip block are enabled
        boolean readyToProvision = false;

        for (GatewayInfo gatewayInfo : gatewayInfos.get(deviceId)) {
            if (gatewayInfo.getGatewayIp().equals(gatewayIp)) {
                //gateway OK, check Ip block
                if (gatewayInfo.containsIp(subscriberIp)) {
                    readyToProvision = true;
                } else {
                    log.error("Gateway " + gatewayIp + " is ready but the ip block for subscriber " + subscriberIp
                            + " has not been provisioned");
                    return;
                }
            }
        }

        if (!readyToProvision) {
            log.error("Gateway " + gatewayIp + " is not provisioned");
            return;
        }

        //Find which table deal with this
        TablesInfo matchingTableInfo = null;
        for (TablesInfo tableInfo : tablesInfos.get(deviceId)) {

            if (tableInfo.containsIp(subscriberIp)) {
                matchingTableInfo = tableInfo;
            }
        }

        if (matchingTableInfo == null) {
            log.error("No table configured for " + subscriberIp + " !! Yet gateway and ip block OK !?!?!!");
            return;
        }

        //Add subscriber to standby list

        SubscriberInfo subInfo = new SubscriberInfo();
        subInfo.setGatewayIp(gatewayIp);
        subInfo.setDownloadSpeed(downloadSpeed);
        subInfo.setUploadSpeed(uploadSpeed);
        subInfo.setTableInfo(matchingTableInfo);

        subInfo.addFlow(subIntercept(subscriberIp, deviceId));

        subscribersInfo.get(deviceId).put(subscriberIp, subInfo);

    }

    public void modifySubscriber(Ip4Address subscriberIp, Ip4Address gatewayIp, int uploadSpeed, int downloadSpeed) {
        //TODO:
        //verify if there is a difference with previous configuration
        //remove former config
        //add new config


    }

    public void addSubscriberFlows(Ip4Address subscriberIp, SubscriberInfo subscriberInfo, TablesInfo tableInfo,
                                   DeviceId deviceId) {

        //TODO : redundancy (link failure)
        GatewayInfo matchingGatewayInfo = null;

        for (GatewayInfo gwInfo : gatewayInfos.get(deviceId)) {
            if (gwInfo.getGatewayIp().equals(subscriberInfo.getGatewayIp())) {
                matchingGatewayInfo = gwInfo;
            }
        }

        if (matchingGatewayInfo == null) {
            log.error("No matching gateway info were found for " + subscriberInfo.getGatewayIp() + " !!?!?!?!?!");
            return;
        }

        for (int i = 0; i < TablesInfo.CONSECUTIVES_TABLES; i++) {

            //Downstream flow


            MeterRequest.Builder downstreamMeter = DefaultMeterRequest.builder();
            downstreamMeter.forDevice(deviceId);
            downstreamMeter.fromApp(appId);
            downstreamMeter.withUnit(Meter.Unit.KB_PER_SEC);
            Band.Builder downstreamBand = DefaultBand.builder();
            downstreamBand.withRate(subscriberInfo.getDownloadSpeed());
            downstreamBand.ofType(Band.Type.DROP);
            downstreamMeter.withBands(Collections.singleton(downstreamBand.build()));

            Meter finalDownstreamMeter = meterService.submit(downstreamMeter.add());


            TrafficSelector.Builder selectorDownStream = DefaultTrafficSelector.builder();
            selectorDownStream.matchIPDscp(TablesInfo.DSCP_LEVELS[i]);
            selectorDownStream.matchIPDst(subscriberIp.toIpPrefix());


            TrafficTreatment.Builder treatmentDownstream = DefaultTrafficTreatment.builder();
            treatmentDownstream.meter(finalDownstreamMeter.id());
            //routing
            treatmentDownstream.setVlanId(subscriberInfo.getCTag());
            treatmentDownstream.setEthSrc(matchingGatewayInfo.getGatewayMac());
            treatmentDownstream.setEthDst(subscriberInfo.getMac());
            treatmentDownstream.setQueue(i);
            treatmentDownstream.setOutput(subscriberInfo.getPort());
            if (i < TablesInfo.CONSECUTIVES_TABLES - 1) {
                treatmentDownstream.transition(tableInfo.getRootTable() + i + 1);
            }

            FlowRule.Builder ruleDownstream = DefaultFlowRule.builder();
            ruleDownstream.withSelector(selectorDownStream.build());
            ruleDownstream.withTreatment(treatmentDownstream.build());
            ruleDownstream.withPriority(SUBSCRIBER_DOWNSTREAM_EXIT_PRIORITY);
            ruleDownstream.forTable(tableInfo.getRootTable() + i);
            ruleDownstream.fromApp(appId);
            ruleDownstream.forDevice(deviceId);
            ruleDownstream.makePermanent();

            FlowRule flowDownstream = ruleDownstream.build();

            flowRuleService.applyFlowRules(flowDownstream);
            subscriberInfo.addFlow(flowDownstream);

            if (i > 0 && i < (TablesInfo.CONSECUTIVES_TABLES - 1)) {

                TrafficSelector.Builder selectorDownStream2 = DefaultTrafficSelector.builder();
                selectorDownStream2.matchIPDst(subscriberIp.toIpPrefix());

                TrafficTreatment.Builder treatmentDownstream2 = DefaultTrafficTreatment.builder();
                treatmentDownstream2.meter(finalDownstreamMeter.id());
                treatmentDownstream2.transition(tableInfo.getRootTable() + i + 1);

                FlowRule.Builder ruleDownstream2 = DefaultFlowRule.builder();
                ruleDownstream2.withSelector(selectorDownStream2.build());
                ruleDownstream2.withTreatment(treatmentDownstream2.build());
                ruleDownstream2.withPriority(SUBSCRIBER_DOWNSTREAM_TRANSITION_PRIORITY);
                ruleDownstream2.forTable(tableInfo.getRootTable() + i);
                ruleDownstream2.fromApp(appId);
                ruleDownstream2.forDevice(deviceId);
                ruleDownstream2.makePermanent();

                FlowRule flowDownstream2 = ruleDownstream2.build();

                flowRuleService.applyFlowRules(flowDownstream2);
                subscriberInfo.addFlow(flowDownstream2);
            }

            //Upstream flow

            MeterRequest.Builder uptreamMeter = DefaultMeterRequest.builder();
            uptreamMeter.forDevice(deviceId);
            uptreamMeter.fromApp(appId);
            uptreamMeter.withUnit(Meter.Unit.KB_PER_SEC);
            Band.Builder upstreamBand = DefaultBand.builder();
            upstreamBand.withRate(subscriberInfo.getDownloadSpeed());
            upstreamBand.ofType(Band.Type.DROP);
            uptreamMeter.withBands(Collections.singleton(upstreamBand.build()));

            Meter finalUpstreamMeter = meterService.submit(downstreamMeter.add());

            TrafficSelector.Builder selectorUpstream = DefaultTrafficSelector.builder();
            selectorUpstream.matchIPDscp(TablesInfo.DSCP_LEVELS[i]);
            selectorUpstream.matchIPSrc(subscriberIp.toIpPrefix());


            TrafficTreatment.Builder treatmentUpstream = DefaultTrafficTreatment.builder();
            treatmentUpstream.meter(finalUpstreamMeter.id());
            treatmentUpstream.popVlan();
            treatmentUpstream.setEthSrc(matchingGatewayInfo.getGatewayMac());
            treatmentUpstream.setEthDst(processor.getMac(devicesConfig.get(deviceId).getPrimaryNextHopIp(), deviceId));
            treatmentUpstream.setQueue(i);
            treatmentUpstream.setOutput(devicesConfig.get(deviceId).getPrimaryLinkPort());
            if (i < TablesInfo.CONSECUTIVES_TABLES - 1) {
                treatmentUpstream.transition(tableInfo.getRootTable() + i + 1);
            }

            FlowRule.Builder ruleUpstream = DefaultFlowRule.builder();
            ruleUpstream.withSelector(selectorUpstream.build());
            ruleUpstream.withTreatment(treatmentUpstream.build());
            ruleUpstream.withPriority(SUBSCRIBER_UPSTREAM_EXIT_PRIORITY);
            ruleUpstream.forTable(tableInfo.getRootTable() + i);
            ruleUpstream.fromApp(appId);
            ruleUpstream.forDevice(deviceId);
            ruleUpstream.makePermanent();


            FlowRule flowUpstream = ruleUpstream.build();

            flowRuleService.applyFlowRules(flowUpstream);
            subscriberInfo.addFlow(flowUpstream);

            if (i > 0 && i < (TablesInfo.CONSECUTIVES_TABLES - 1)) {

                TrafficSelector.Builder selectorUpstream2 = DefaultTrafficSelector.builder();
                selectorUpstream2.matchIPSrc(subscriberIp.toIpPrefix());


                TrafficTreatment.Builder treatmentUpstream2 = DefaultTrafficTreatment.builder();
                treatmentUpstream2.meter(finalUpstreamMeter.id());
                treatmentUpstream2.transition(tableInfo.getRootTable() + i + 1);

                FlowRule.Builder ruleUpstream2 = DefaultFlowRule.builder();
                ruleUpstream2.withSelector(selectorUpstream2.build());
                ruleUpstream2.withTreatment(treatmentUpstream2.build());
                ruleUpstream2.withPriority(SUBSCRIBER_UPSTREAM_TRANSITION_PRIORITY);
                ruleUpstream2.forTable(tableInfo.getRootTable() + i);
                ruleUpstream2.fromApp(appId);
                ruleUpstream2.forDevice(deviceId);
                ruleUpstream2.makePermanent();

                FlowRule flowUpstream2 = ruleUpstream2.build();

                flowRuleService.applyFlowRules(flowUpstream2);
                subscriberInfo.addFlow(flowUpstream2);

            }
        }
    }




    public void tableSplit(TablesInfo tableInfo, Ip4Prefix ipBlock, DeviceId deviceId) {


        for (int i = 0; i < TablesInfo.CONSECUTIVES_TABLES; i++) {

            //Downstream flow

            TrafficSelector.Builder selectorDownStream = DefaultTrafficSelector.builder();
            selectorDownStream.matchIPDscp(TablesInfo.DSCP_LEVELS[i]);
            selectorDownStream.matchIPDst(ipBlock);


            TrafficTreatment.Builder treatmentDownstream = DefaultTrafficTreatment.builder();
            treatmentDownstream.transition(tableInfo.getRootTable() + i);

            FlowRule.Builder ruleDownstream = DefaultFlowRule.builder();
            ruleDownstream.withSelector(selectorDownStream.build());
            ruleDownstream.withTreatment(treatmentDownstream.build());
            ruleDownstream.withPriority(DOWNSTREAM_SPLIT);
            ruleDownstream.forTable(0);
            ruleDownstream.fromApp(appId);
            ruleDownstream.forDevice(deviceId);
            ruleDownstream.makePermanent();

            flowRuleService.applyFlowRules(ruleDownstream.build());

            //Upstream flow

            TrafficSelector.Builder selectorUpstream = DefaultTrafficSelector.builder();
            selectorUpstream.matchIPDscp(TablesInfo.DSCP_LEVELS[i]);
            selectorUpstream.matchIPSrc(ipBlock);


            TrafficTreatment.Builder treatmentUpstream = DefaultTrafficTreatment.builder();
            treatmentUpstream.transition(tableInfo.getRootTable() + i);

            FlowRule.Builder ruleUpstream = DefaultFlowRule.builder();
            ruleUpstream.withSelector(selectorUpstream.build());
            ruleUpstream.withTreatment(treatmentUpstream.build());
            ruleUpstream.withPriority(UPSTREAM_SPLIT);
            ruleUpstream.forTable(0);
            ruleUpstream.fromApp(appId);
            ruleUpstream.forDevice(deviceId);
            ruleUpstream.makePermanent();

            flowRuleService.applyFlowRules(ruleDownstream.build());

        }
    }


/*
    private void downstreamCtag(int sTag, int cTag, DeviceId deviceId) {

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchIPDst(tagsToIpMatching(sTag, cTag).toIpPrefix());


        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.setVlanId(VlanId.vlanId((short) cTag));
        treatment.transition(11);

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(2000);
        rule.forTable(10);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        flowRuleService.applyFlowRules(rule.build());


    }

    private void downstreamStag(int sTag, int cTag, int port, int kbps, DeviceId deviceId) {

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


        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchIPDst(tagsToIpMatching(sTag, cTag).toIpPrefix());


        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.pushVlan();
        treatment.deferred();
        treatment.setVlanId(VlanId.vlanId((short) sTag));
        treatment.setQueue(3);
        treatment.meter(finalMeter.id());
        treatment.setOutput(PortNumber.portNumber(port));


        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(2000);
        rule.forTable(11);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        flowRuleService.applyFlowRules(rule.build());


    }

    private void downstreamTrafficHandling(DeviceId deviceId){

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchInPort(uplinkPort);


        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.pushVlan();
        treatment.transition(10);


        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(5000);
        rule.forTable(0);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        flowRuleService.applyFlowRules(rule.build());
    }

    private void sTagMatch(int port, int sTag){


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

    private void aclDropFlow(Ip4Address dstIP, Ip4Address srcIp){

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



    private void meterAssignmentAndOut(int sTag, int cTag, int kbps){

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
        treatment.setEthDst(uplinkMac);
        treatment.meter(finalMeter.id());
        treatment.setOutput(uplinkPort);
        treatment.setQueue(3);


        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(1);
        rule.forTable(4);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        flowRuleService.applyFlowRules(rule.build());

    }*/

    // ------------------------------------------------------------

    //Intercepts

    private void arpIntercept(Ip4Address respondFor, DeviceId deviceId) {


        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_ARP);
        selector.matchArpTpa(respondFor);


        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.punt();

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(ARP_INTERCEPT_PRIORITY);
        rule.forTable(0);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        flowRuleService.applyFlowRules(rule.build());
    }

    private void icmpIntercept(Ip4Address respondFor, DeviceId deviceId) {

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchIPDst(respondFor.toIpPrefix());
        selector.matchIPProtocol(IPv4.PROTOCOL_ICMP);


        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.punt();

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(ICMP_INTERCEPT_PRIORITY);
        rule.forTable(0);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        flowRuleService.applyFlowRules(rule.build());

    }

    private FlowRule subIntercept(Ip4Address catchIp, DeviceId deviceId) {

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchIPSrc(catchIp.toIpPrefix());


        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.punt();

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(SUB_INTERCEPT_PRIORITY);
        rule.forTable(0);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        FlowRule flow = rule.build();

        flowRuleService.applyFlowRules(flow);

        return flow;

    }

    private void clearIntercepts(DeviceId deviceId) {

        Iterable<FlowRule> flows = flowRuleService.getFlowRulesById(appId);

        for (FlowRule flow :flows) {

            if (flow.deviceId().equals(deviceId)) {

                if (flow.selector().getCriterion(Criterion.Type.ARP_TPA) != null) {
                    //ARP intercepts
                    flowRuleService.removeFlowRules(flow);
                } else if (flow.selector().getCriterion(Criterion.Type.IP_PROTO) != null) {
                    IPProtocolCriterion proto = (IPProtocolCriterion) flow.selector()
                            .getCriterion(Criterion.Type.IP_PROTO);
                    if (proto.protocol() == 1 || proto.protocol() == 2) {
                        //ICMP or IGMP intercepts
                        flowRuleService.removeFlowRules(flow);
                    }
                }
            }

        }
    }

    //--------------------------------------------------------

    //Config


    public void checkNewConfig(BngDeviceConfig aggConfig) {

        BngDeviceConfig oldConfig = devicesConfig.get(aggConfig.getDeviceId());

        if (oldConfig != null) {
            //A config already exist for this device
            //Check if different
            if (aggConfig.equals(oldConfig)) {
                log.info("This config for device " + aggConfig.getDeviceId() + " has not changed");
                return;
            } else {
                log.info("Config for device " + aggConfig.getDeviceId() + " has been modified");
                newConfig(aggConfig);
                devicesConfig.replace(aggConfig.getDeviceId(), aggConfig);
            }
        } else {
            // Config did not exist
            log.info("New config for device " + aggConfig.getDeviceId());
            newConfig(aggConfig);
            devicesConfig.put(aggConfig.getDeviceId(), aggConfig);

        }

    }

    private void newConfig(BngDeviceConfig config) {


        DeviceId deviceId = config.getDeviceId();
        log.info("Ready to remove old config for " + deviceId);

        //Clean up old knowledge
        clearIntercepts(deviceId);

        processor.clearRoutingInfo(deviceId);

        //For now do not clear it everytime : ??
        //Only initiate it for the first time
        if (!subscribersInfo.containsKey(deviceId)) {
            subscribersInfo.put(deviceId, new HashMap<>());
        }
        
        if (!tablesInfos.containsKey(deviceId)) {
            List<TablesInfo> deviceTablesInfo = new LinkedList<>();
            deviceTablesInfo.add(new TablesInfo(1));
            tablesInfos.put(deviceId, deviceTablesInfo);
        }

        if (!gatewayInfos.containsKey(deviceId)) {
            gatewayInfos.put(deviceId, new LinkedList<>());
        }

/*        //TODO : multicast
        getMulticastHandler(deviceId).kill();
        removeMulticastHandler(deviceId);*/

        //TODO : link failure detection
        //linkFailureDetection.removeDevice(deviceId);

        log.info("Old knowledge cleared");


        //New Knowledge

        //IPs the agg switch is responding to ARP
        arpIntercept(config.getPrimaryLinkIp(), deviceId);
        arpIntercept(config.getSecondaryLinkIp(), deviceId);

        //IPs the agg switch is responding to ping
        icmpIntercept(config.getLoopbackIP(), deviceId);
        icmpIntercept(config.getPrimaryLinkIp(), deviceId);
        icmpIntercept(config.getSecondaryLinkIp(), deviceId);

        log.info("New intercepts set up");

        //loopback
        processor.addRoutingInfo(deviceId, config.getPrimaryLinkPort(), Ip4Prefix.valueOf(config.getLoopbackIP(), 24),
                config.getLoopbackIP(), MacAddress.valueOf("00:00:00:00:00:00"));
        processor.addRoutingInfo(deviceId, config.getSecondaryLinkPort(), Ip4Prefix.valueOf(config.getLoopbackIP(), 24),
                config.getLoopbackIP(), MacAddress.valueOf("00:00:00:00:00:00"));
        //Uplinks
        processor.addRoutingInfo(deviceId, config.getPrimaryLinkPort(), config.getPrimaryLinkSubnet(),
                config.getPrimaryLinkIp(), config.getPrimaryLinkMac());
        processor.addRoutingInfo(deviceId, config.getSecondaryLinkPort(), config.getSecondaryLinkSubnet(),
                config.getSecondaryLinkIp(), config.getSecondaryLinkMac());

        log.info("Routing infos added");

       /*
        //TODO : multicast
        igmpIntercept(deviceId);
        addMulticastHandler(deviceId);

        log.info("Multicast handler added");*/

        //TODO : LinkFailureDetection

        //linkFailureDetection.addRedundancyPort(new ConnectPoint(deviceId, config.getPrimaryLinkPort()));
        //linkFailureDetection.addRedundancyPort(new ConnectPoint(deviceId, config.getSecondaryLinkPort()));

        log.info("Link failure detection set up");

    }

    //--------------------------------------------------------------------------------

    //Link failure

    public void notifyFailure(DeviceId deviceId, PortNumber port, MacAddress oldMac) {
        log.warn("Failure detected :  device " + deviceId + ", port " + port);
        log.error("Link Failure detection not yet implemented : notifyFailure()");
        //TODO : link failure detection
        //linkFailureDetection.event(deviceId, port, true, false, oldMac, null);
    }

    public void notifyRecovery(DeviceId deviceId, PortNumber port, MacAddress oldMac, MacAddress newDstMac) {
        log.warn("Recovery detected :  device " + deviceId + ", port " + port + " changing MAC form " + oldMac +
                " to " + newDstMac);
        log.error("Link Failure detection not yet implemented : notifyRecovery()");
        //TODO: link failure detection
        //linkFailureDetection.event(deviceId, port, false, true, oldMac, newDstMac);
    }

    public void notifyRecovery(DeviceId deviceId, PortNumber port, MacAddress oldMac) {
        log.warn("Recovery detected :  device " + deviceId + ", port " + port);
        log.error("Link Failure detection not yet implemented : notifyRecovery()");
        //TODO: link failure detection
        //linkFailureDetection.event(deviceId, port, false, false, oldMac, null);
    }

    public void notifyMacChange(DeviceId deviceId, PortNumber port, MacAddress oldMac, MacAddress newDstMac) {
        log.error("Link Failure detection not yet implemented : notifyMacChange()");
        //TODO: link failure detection
        notifyFailure(deviceId, port, oldMac);
        notifyRecovery(deviceId, port, oldMac, newDstMac);
    }





}


