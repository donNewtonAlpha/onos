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

package org.onosproject.noviaggswitch;


import org.apache.felix.scr.annotations.*;


import org.onlab.packet.*;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.driver.extensions.NoviflowPopVxLan;
import org.onosproject.driver.extensions.NoviflowSetVxLan;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.*;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.flow.*;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.group.*;
import org.onosproject.net.flow.criteria.*;
import org.onosproject.net.meter.*;
import org.onosproject.net.packet.*;
import org.onosproject.noviaggswitch.config.AggDeviceConfig;
import org.onosproject.noviaggswitch.config.NoviAggSwitchConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * Created by nick on 6/26/15.
 */
@Component(immediate = true)

public class NoviAggSwitchComponent {


    static final Logger log = LoggerFactory.getLogger(NoviAggSwitchComponent.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MeterService meterService;  
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;


/*    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;*/


    private static final int ARP_INTERCEPT_PRIORITY = 15000;
    private static final int ICMP_INTERCEPT_PRIORITY = 14000;
    private static final int IGMP_INTERCEPT_PRIORITY = 13000;
    private static final int ENCAPSULATION_PRIORITY = 1600;
    private static final int DECAPSULATION_PRIORITY = 1000;


    static ApplicationId appId;


  /*  static final DeviceId noviflow = DeviceId.deviceId("of:000000223d5a00d9");

    private static MacAddress switchMac = MacAddress.valueOf("68:05:33:44:55:66");
    private static Ip4Address aggSwitchIP = Ip4Address.valueOf("10.50.1.1");
    private static Ip4Address primaryLinkIP = Ip4Address.valueOf("10.10.1.0");
    private static Ip4Address secondaryLinlkIP = Ip4Address.valueOf("10.10.2.0");
    private PortNumber bngPort =  PortNumber.portNumber(7);
    private PortNumber secondaryBngPort = PortNumber.portNumber(8);*/


    private NoviAggSwitchPacketProcessor processor;
    private LinkFailureDetection linkFailureDetection;
    private NoviAggSwitchConfigListener cfgListener;

    private HashMap<DeviceId, MulticastHandler> multicastHandlers;
    private HashMap<DeviceId, AggDeviceConfig> devicesConfig;


    private static NoviAggSwitchComponent instance = null;

    public static NoviAggSwitchComponent getComponent() {
        return instance;

    }



    @Activate
    protected void activate() {

        log.debug("trying to activate");
        instance = this;
        appId = coreService.registerApplication("org.onosproject.noviaggswitch");

        multicastHandlers = new HashMap<>();
        devicesConfig = new HashMap<>();


/*        //Config
        cfgListener = new NoviAggSwitchConfigListener();
        cfgService.registerConfigFactory(cfgListener.getCfgAppFactory());
        cfgService.addListener(cfgListener);*/




        //Packet processor
        processor = new NoviAggSwitchPacketProcessor(packetService);
        packetService.addProcessor(processor, 1);
        processor.startARPingThread();

        linkFailureDetection = new LinkFailureDetection(flowRuleService, new LinkedList<>());
        //deviceService.addListener(linkFailureDetection);

       /* //Routing info
        //4 info

        //loopback
        processor.addRoutingInfo(deviceId, bngPort, Ip4Prefix.valueOf(aggSwitchIP, 24), aggSwitchIP, MacAddress.valueOf("00:00:00:00:00:00"));
        processor.addRoutingInfo(deviceId, secondaryBngPort, Ip4Prefix.valueOf(aggSwitchIP, 24), aggSwitchIP, MacAddress.valueOf("00:00:00:00:00:00"));
        //Uplinks
        processor.addRoutingInfo(deviceId, bngPort, Ip4Prefix.valueOf(primaryLinkIP, 31), primaryLinkIP, MacAddress.valueOf("68:05:11:11:11:11"));
        processor.addRoutingInfo(deviceId, secondaryBngPort, Ip4Prefix.valueOf(secondaryLinlkIP, 31), secondaryLinlkIP, MacAddress.valueOf("68:05:22:22:22:22"));


        //LinkFailureDetection
        List<ConnectPoint> redundancyPorts = new LinkedList<>();
        redundancyPorts.add(new ConnectPoint(deviceId, bngPort));
        redundancyPorts.add(new ConnectPoint(deviceId, secondaryBngPort));





        //IPs the agg switch is responding to ARP
        //arpIntercept(aggSwitchIP);
        arpIntercept(primaryLinkIP, deviceId);
        arpIntercept(secondaryLinlkIP, deviceId);

        //IPs the agg switch is responding to ping
        icmpIntercept(aggSwitchIP, deviceId);
        icmpIntercept(primaryLinkIP, deviceId);
        icmpIntercept(secondaryLinlkIP, deviceId);



        //Tunnel
        Random rand = new Random();
        int udpPort = rand.nextInt() + 2000;
        int udpPort2 = rand.nextInt() + 2000;


        addAccessDevice(deviceId, 5, 5002, udpPort, "10.50.1.2", "10.10.1.1", "10.10.2.1", aggSwitchIP, MacAddress.valueOf("68:05:11:11:11:11"), MacAddress.valueOf("68:05:22:22:22:22"));
        addAccessDevice(deviceId, 9, 5003, udpPort2, "10.50.1.3", "10.10.1.1", "10.10.2.1", aggSwitchIP, MacAddress.valueOf("68:05:11:11:11:11"), MacAddress.valueOf("68:05:22:22:22:22"));

*/

        /*
        //Multicast handler

        igmpIntercept();
        addMulticastHandler(deviceId);


        */
        //awsCloudShitHook(noviflow);

        log.info("NoviFlow AggSwitch activated");

    }


    @Deactivate
    protected void deactivate() {

        packetService.removeProcessor(processor);
        processor.stopARPingThread();


        flowRuleService.removeFlowRulesById(appId);

        try {

           /* cfgService.unregisterConfigFactory(cfgListener.getCfgAppFactory());
            cfgService.removeListener(cfgListener);*/

            //NoviAggSwitchConfig config = (NoviAggSwitchConfig) cfgService.getConfig(appId, NoviAggSwitchConfig.class);

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

/*    public void addAccessDevice(int port, int vni, String bngVxlanIp) {

        addAccessDevice(port, vni, bngVxlanIp, bngVxlanIp, bngVxlanIp);

    }*/

    public void addAccessDevice(DeviceId deviceId, int port, int vni, String bngVxlanIp, String viaPrimaryIP, String viaSecondaryIP) {

        Random rand = new Random();
        int udpPort = rand.nextInt() + 2000;

        //try {

            //NoviAggSwitchConfig config = (NoviAggSwitchConfig) cfgService.getConfig(appId, NoviAggSwitchConfig.class);

            addAccessDevice(deviceId, port, vni, udpPort, bngVxlanIp, viaPrimaryIP, viaSecondaryIP, devicesConfig.get(deviceId).getLoopbackIP(), devicesConfig.get(deviceId).getPrimaryLinkMac(), devicesConfig.get(deviceId).getSecondaryLinkMac());
        /*} catch (Exception e) {

            addAccessDevice(deviceId, port, vni, udpPort, bngVxlanIp, viaPrimaryIP, viaSecondaryIP, aggSwitchIP, MacAddress.valueOf("68:05:11:11:11:11"), MacAddress.valueOf("68:05:11:11:11:22"));


        }*/

    }

    private void addAccessDevice(DeviceId deviceId, int port, int vni, int udpPort, String bngVxlanIp, String viaPrimaryIP, String viaSecondaryIP,
                                 Ip4Address switchVxlanIp, MacAddress primaryLinkSwitchMac, MacAddress secondaryLinkSwitchMac) {

        Runnable r = new Runnable() {

            @Override
            public void run() {
                Ip4Address bngVxLanIP = Ip4Address.valueOf(bngVxlanIp);


                List<VxLanTunnel> tunnels = getTunnels(deviceId);
                for (VxLanTunnel tunnel : tunnels) {
                    if (tunnel.match(bngVxLanIP, vni)) {
                        log.warn("Tunnel already exists !");
                    }
                }

                VxlanTunnelId tunnelId = new VxlanTunnelId(deviceId, bngVxLanIP, vni);

                //PrimaryPath
                MacAddress bngVxLanPrimaryMac = processor.getMac(Ip4Address.valueOf(viaPrimaryIP), tunnelId);
                if (bngVxLanPrimaryMac != null) {
                    log.info("MAC found, ready to add flows");

                    try {
                        accessToBng(deviceId, PortNumber.portNumber(port), vni, udpPort, bngVxLanIP, bngVxLanPrimaryMac, switchVxlanIp, primaryLinkSwitchMac, true);
                        bngToAccess(deviceId, PortNumber.portNumber(port), vni, Ip4Address.valueOf(bngVxlanIp), true);

                    } catch (Exception e) {
                        log.warn("Exception", e);
                    }

                    //SecondaryPath if different from primary

                    if (!viaSecondaryIP.equals(viaPrimaryIP)) {

                        MacAddress bngVxLanSecondaryMac = processor.getMac(Ip4Address.valueOf(viaSecondaryIP), tunnelId);
                        if (bngVxLanSecondaryMac != null) {
                            log.info("MAC found, ready to add flows");

                            try {
                                accessToBng(deviceId, PortNumber.portNumber(port), vni, udpPort, bngVxLanIP, bngVxLanSecondaryMac, switchVxlanIp, secondaryLinkSwitchMac, false);
                                bngToAccess(deviceId, PortNumber.portNumber(port), vni, Ip4Address.valueOf(bngVxlanIp), false);

                            } catch (Exception e) {
                                log.warn("Exception", e);
                            }
                        }
                    }
                }
            }
        };

        Thread t = new Thread(r);
        t.setDaemon(true);
        t.start();

    }



    private void accessToBng(DeviceId deviceId, PortNumber port, int vni, int udpPort, Ip4Address bngVxlanIp, MacAddress bngVxlanMac, Ip4Address switchVxlanIp, MacAddress switchVxlanMac, boolean primary){


        PortNumber primaryPort;
        PortNumber secondaryPort;

        try {
            //NoviAggSwitchConfig config = (NoviAggSwitchConfig) cfgService.getConfig(appId, NoviAggSwitchConfig.class);
            primaryPort = devicesConfig.get(deviceId).getPrimaryLinkPort();
            secondaryPort = devicesConfig.get(deviceId).getSecondaryLinkPort();
        } catch (Exception e) {
            log.warn("Config unavailable");
            return;
            /*primaryPort = bngPort;
            secondaryPort = secondaryBngPort;*/
        }

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchInPort(port);


        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.extension(new NoviflowSetVxLan(switchVxlanMac,bngVxlanMac, switchVxlanIp, bngVxlanIp, udpPort, vni), deviceId);
        if (primary) {
            treatment.setOutput(primaryPort);
        } else {
            treatment.setOutput(secondaryPort);
        }

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        if (primary) {
            rule.withPriority(ENCAPSULATION_PRIORITY);
        } else {
            rule.withPriority(ENCAPSULATION_PRIORITY/2);
        }
        rule.forTable(0);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        flowRuleService.applyFlowRules(rule.build());

    }


    private void bngToAccess(DeviceId deviceId, PortNumber port, int vni, Ip4Address bngVxLanIP, boolean primary) {

        PortNumber primaryPort;
        PortNumber secondaryPort;

        try {
            //NoviAggSwitchConfig config = (NoviAggSwitchConfig) cfgService.getConfig(appId, NoviAggSwitchConfig.class);
            primaryPort = devicesConfig.get(deviceId).getPrimaryLinkPort();
            secondaryPort = devicesConfig.get(deviceId).getSecondaryLinkPort();
        } catch (Exception e) {
            log.warn("Config unavailable");
            return;
        }

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        if (primary) {
            selector.matchInPort(primaryPort);
        } else {
            selector.matchInPort(secondaryPort);
        }
        selector.matchIPSrc(bngVxLanIP.toIpPrefix());


        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.extension(new NoviflowPopVxLan(), deviceId);
        treatment.setOutput(port);

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        if (primary) {
            rule.withPriority(DECAPSULATION_PRIORITY);
        } else {
            rule.withPriority(DECAPSULATION_PRIORITY/2);
        }
        rule.forTable(0);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        flowRuleService.applyFlowRules(rule.build());

    }

    public void removeTunnel(DeviceId deviceId, Ip4Address vxlanIP, int vni) {

        List<VxLanTunnel> tunnels = getTunnels(deviceId);
        for (VxLanTunnel tunnel : tunnels) {
            if (tunnel.match(vxlanIP, vni)) {
                for (FlowRule flow : tunnel.getFlows()) {
                    flowRuleService.removeFlowRules(flow);
                }
            }
        }

        checkNeedForMacCheck(deviceId, vxlanIP, vni);

    }

    public void removeAllTunnels() {

        Set<DeviceId> devices = multicastHandlers.keySet();
        for (DeviceId deviceId : devices) {
            removeAllTunnels(deviceId);
        }
    }

    public void removeAllTunnels(DeviceId deviceId) {

        Iterable<FlowRule> flows = flowRuleService.getFlowRulesById(appId);
        for (FlowRule flow : flows) {

            if (flow.deviceId().equals(deviceId)) {

                List<Instruction> instructions = flow.treatment().immediate();
                for (Instruction instruction : instructions) {

                    if (instruction.type() == Instruction.Type.EXTENSION) {

                        try {
                            Instructions.ExtensionInstructionWrapper extensionInstruction = (Instructions.ExtensionInstructionWrapper) instruction;
                            ExtensionTreatment extension = extensionInstruction.extensionInstruction();

                            if (extension.type().equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NOVIFLOW_SET_VXLAN.type())) {

                                flowRuleService.removeFlowRules(flow);

                            } else if (extension.type().equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NOVIFLOW_POP_VXLAN.type())) {

                                flowRuleService.removeFlowRules(flow);

                            }

                        } catch (Exception e) {
                            log.warn("remove tunnel exception", e);
                        }
                    }

                }
            }

        }
        log.info("Tunnels for device " + deviceId + " have been removed");

        //processor.clearMacChecks(deviceId);
        processor.clearMacRequests(deviceId);
    }

    public void checkNeedForMacCheck(DeviceId deviceId, Ip4Address vxlanIp, int vni) {

        try {
            Thread.sleep(100);
        } catch (Exception e){

        }
       /* List<VxLanTunnel> tunnels = getTunnels(deviceId);
        boolean needToRemove = true;

        for(VxLanTunnel tunnel : tunnels) {
            if(tunnel.isValid()) {
                needToRemove = false;
            }
        }

        if(needToRemove) {
            log.info("Removing MacChecks and MacRequests for device " + deviceId);
            processor.clearMacChecks(deviceId);
            processor.clearMacRequests(deviceId);
        }*/
       VxlanTunnelId tunnelId = new VxlanTunnelId(deviceId, vxlanIp, vni);
       processor.removeRequestingTunnel(tunnelId);
    }

    public List<VxLanTunnel> getTunnels(DeviceId deviceId) {
        Iterable<FlowRule> flows = flowRuleService.getFlowRulesById(appId);
        return getTunnels(deviceId, flows);

    }

    public List<VxLanTunnel> getTunnels(DeviceId deviceId, Iterable<FlowRule> flows) {

        log.debug("get tunnel fonction");

        List<VxLanTunnel> tunnels = new LinkedList<>();


        for (FlowRule flow : flows) {

            if (flow.deviceId().equals(deviceId)) {

                List<Instruction> instructions = flow.treatment().immediate();
                for (Instruction instruction : instructions) {

                    if (instruction.type() == Instruction.Type.EXTENSION) {


                        try {
                            Instructions.ExtensionInstructionWrapper extensionInstruction = (Instructions.ExtensionInstructionWrapper) instruction;
                            ExtensionTreatment extension = extensionInstruction.extensionInstruction();

                            log.debug("Extension treatment : " + extension.toString());

                            if (extension.type().equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NOVIFLOW_SET_VXLAN.type())) {
                                log.debug("set vxlan extension found");

                                NoviflowSetVxLan noviflowVxlan = (NoviflowSetVxLan) extension;
                                Criterion criterion = flow.selector().getCriterion(Criterion.Type.IN_PORT);
                                PortNumber aggPort = ((PortCriterion) criterion).port();
                                Ip4Address vxlanIp = noviflowVxlan.getDstIp();
                                int vni = noviflowVxlan.getVxLanId();
                                MacAddress dstMac = noviflowVxlan.getDstMac();

                                boolean needNew = true;
                                for (VxLanTunnel tunnel : tunnels) {

                                    if (tunnel.match(vxlanIp)) {
                                        needNew = false;
                                        tunnel.addFlow(flow);
                                        tunnel.setVni(vni);
                                        tunnel.setPort(aggPort);
                                        tunnel.setViaMac(flow.priority(), dstMac);
                                    }

                                }
                                if (needNew) {
                                    VxLanTunnel newTunnel = new VxLanTunnel(vxlanIp);
                                    newTunnel.setVni(vni);
                                    newTunnel.setPort(aggPort);
                                    newTunnel.setViaMac(flow.priority(), dstMac);
                                    newTunnel.addFlow(flow);
                                    tunnels.add(newTunnel);
                                }


                            } else if (extension.type().equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NOVIFLOW_POP_VXLAN.type())) {
                                log.debug("pop vxlan extension found");

                                //look in the selector for the IP

                                Ip4Address vxlanIp = ((IPCriterion) flow.selector().getCriterion(Criterion.Type.IPV4_SRC)).ip().getIp4Prefix().address();

                                boolean needNew = true;
                                for (VxLanTunnel tunnel : tunnels) {

                                    if (tunnel.match(vxlanIp)) {
                                        needNew = false;
                                        tunnel.addFlow(flow);
                                    }

                                }
                                if (needNew) {
                                    VxLanTunnel newTunnel = new VxLanTunnel(vxlanIp);
                                    newTunnel.addFlow(flow);
                                    tunnels.add(newTunnel);
                                }

                            }

                        } catch (Exception e) {
                            log.warn("get tunnels exception", e);
                        }
                    }

                }
            }

        }

        int valid = 0;
        int invalid = 0;

        for (VxLanTunnel tunnel : tunnels) {
            if (tunnel.isValid()) {
                valid++;
            } else {
                invalid++;
            }
        }

        log.info(valid + " valid tunnels found and " + invalid + " invalid ones");

        return tunnels;


    }

    public Set<DeviceId> getAggDevices() {
        return devicesConfig.keySet();
    }



    private void arpIntercept(Ip4Address respondFor, DeviceId deviceId) {
        
        //TODO : make a table rule and integrate IGMP  (table 20) ?

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

    private void igmpIntercept(DeviceId deviceId) {

        //NoviAggSwitchConfig config = (NoviAggSwitchConfig) cfgService.getConfig(appId, NoviAggSwitchConfig.class);

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchIPProtocol(IPv4.PROTOCOL_IGMP);


        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.setOutput(devicesConfig.get(deviceId).getPrimaryLinkPort());
        treatment.punt();

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(IGMP_INTERCEPT_PRIORITY);
        rule.forTable(0);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        flowRuleService.applyFlowRules(rule.build());

    }

    private void ipBypass(DeviceId deviceId, Ip4Address ip, PortNumber srcPort, PortNumber dstPort) {

        //To SrcPort

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchInPort(dstPort);
        selector.matchIPDst(ip.toIpPrefix());


        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.setOutput(srcPort);

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(50000);
        rule.forTable(0);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        flowRuleService.applyFlowRules(rule.build());

        //To DstPort

        selector = DefaultTrafficSelector.builder();
        selector.matchInPort(srcPort);
        selector.matchIPDst(ip.toIpPrefix());


        treatment = DefaultTrafficTreatment.builder();
        treatment.setOutput(dstPort);


        rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(50001);
        rule.forTable(0);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        flowRuleService.applyFlowRules(rule.build());


    }


    private void addMulticastHandler(DeviceId deviceId) {

        MulticastHandler multicastHandler = new MulticastHandler(deviceId, flowRuleService, groupService, appId);

        MulticastGroupCleaningThread thread = new MulticastGroupCleaningThread(multicastHandler, groupService);
        multicastHandler.setThread(thread);

        thread.setDaemon(true);
        thread.start();

        multicastHandlers.put(deviceId, multicastHandler);

    }

    private void removeMulticastHandler(DeviceId deviceId){
        multicastHandlers.remove(deviceId);
    }

    public MulticastHandler getMulticastHandler(DeviceId deviceId) {

        return multicastHandlers.get(deviceId);

    }

    private void clearIntercepts(DeviceId deviceId) {

        Iterable<FlowRule> flows = flowRuleService.getFlowRulesById(appId);

        for (FlowRule flow :flows) {

            if (flow.deviceId().equals(deviceId)) {

                if (flow.selector().getCriterion(Criterion.Type.ARP_TPA) != null) {
                    //ARP intercepts
                    flowRuleService.removeFlowRules(flow);
                } else if (flow.selector().getCriterion(Criterion.Type.IP_PROTO) != null){
                    IPProtocolCriterion proto = (IPProtocolCriterion) flow.selector().getCriterion(Criterion.Type.IP_PROTO);
                    if (proto.protocol() == 1 || proto.protocol() == 2) {
                        //ICMP or IGMP intercepts
                        flowRuleService.removeFlowRules(flow);
                    }
                }
            }

        }
    }

    public void checkNewConfig(AggDeviceConfig aggConfig) {

        AggDeviceConfig oldConfig = devicesConfig.get(aggConfig.getDeviceId());

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

    private void newConfig(AggDeviceConfig config) {


        DeviceId deviceId = config.getDeviceId();
        log.info("Ready to remove old config for " + deviceId);

        //Clean up old knowledge
        clearIntercepts(deviceId);

        processor.clearRoutingInfo(deviceId);

/*        //TODO
        getMulticastHandler(deviceId).kill();
        removeMulticastHandler(deviceId);*/

        List<VxLanTunnel> tunnels = getTunnels(deviceId);
        removeAllTunnels(deviceId);

        linkFailureDetection.removeDevice(deviceId);

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
        processor.addRoutingInfo(deviceId, config.getPrimaryLinkPort(), Ip4Prefix.valueOf(config.getLoopbackIP(), 24), config.getLoopbackIP(), MacAddress.valueOf("00:00:00:00:00:00"));
        processor.addRoutingInfo(deviceId, config.getSecondaryLinkPort(), Ip4Prefix.valueOf(config.getLoopbackIP(), 24), config.getLoopbackIP(), MacAddress.valueOf("00:00:00:00:00:00"));
        //Uplinks
        processor.addRoutingInfo(deviceId, config.getPrimaryLinkPort(), config.getPrimaryLinkSubnet(), config.getPrimaryLinkIp(), config.getPrimaryLinkMac());
        processor.addRoutingInfo(deviceId, config.getSecondaryLinkPort(), config.getSecondaryLinkSubnet(), config.getSecondaryLinkIp(), config.getSecondaryLinkMac());

        log.info("Routing infos added");

       /*
        //TODO
        igmpIntercept(deviceId);
        addMulticastHandler(deviceId);

        log.info("Multicast handler added");*/

        //LinkFailureDetection

        linkFailureDetection.addRedundancyPort(new ConnectPoint(deviceId, config.getPrimaryLinkPort()));
        linkFailureDetection.addRedundancyPort(new ConnectPoint(deviceId, config.getSecondaryLinkPort()));

        log.info("Link failure detection set up");


        //Reinstate tunnels

        Random rand = new Random();

        int validTunnels = 0;
        int invalidTunnels = 0;

        for (VxLanTunnel tunnel : tunnels) {

            if (tunnel.isValid()) {
                validTunnels ++;

                int udpPort = rand.nextInt() + 2000;

                accessToBng(deviceId, tunnel.getPort(), tunnel.getVni(), udpPort, tunnel.getDstIp(), tunnel.getPrimaryViaMac(), config.getLoopbackIP(), config.getPrimaryLinkMac(), true);
                bngToAccess(deviceId, tunnel.getPort(), tunnel.getVni(), tunnel.getDstIp(), true);
                accessToBng(deviceId, tunnel.getPort(), tunnel.getVni(), udpPort, tunnel.getDstIp(), tunnel.getSecondaryViaMac(), config.getLoopbackIP(), config.getSecondaryLinkMac(), false);
                bngToAccess(deviceId, tunnel.getPort(), tunnel.getVni(), tunnel.getDstIp(), false);
            } else {
                invalidTunnels++;
            }
        }

        log.info(validTunnels + " tunnels have been reset,  " + invalidTunnels + " tunnels were invalid" );


    }

    public void notifyFailure(DeviceId deviceId, PortNumber port, MacAddress oldMac) {
        log.warn("Failure detected :  device " + deviceId +", port " + port);
        linkFailureDetection.event(deviceId, port, true, false, oldMac, null);
    }

    public void notifyRecovery(DeviceId deviceId, PortNumber port, MacAddress oldMac, MacAddress newDstMac) {
        log.warn("Recovery detected :  device " + deviceId +", port " + port + " changing MAC form " + oldMac + " to " + newDstMac);
        linkFailureDetection.event(deviceId, port, false, true, oldMac, newDstMac);
    }

    public void notifyRecovery(DeviceId deviceId, PortNumber port, MacAddress oldMac) {
        log.warn("Recovery detected :  device " + deviceId +", port " + port);
        linkFailureDetection.event(deviceId, port, false, false, oldMac, null);
    }

    public void notifyMacChange(DeviceId deviceId, PortNumber port, MacAddress oldMac, MacAddress newDstMac) {
        notifyFailure(deviceId, port, oldMac);
        notifyRecovery(deviceId, port, oldMac, newDstMac);
    }


    public void awsCloudShitHook(DeviceId deviceId) {

        log.info("Cloud shithook preparation ....");

        PortNumber rgServer = PortNumber.portNumber(27);
        PortNumber awsUplink = PortNumber.portNumber(28);


        Ip4Address vxlanLoopback = Ip4Address.valueOf("10.64.11.20");
        MacAddress switchMac = MacAddress.valueOf("68:05:11:11:11:11");

        Ip4Address vxlanDstIp = Ip4Address.valueOf("52.44.253.136");
        int vni = 5050;

        processor.addRoutingInfo(deviceId, awsUplink, Ip4Prefix.valueOf(vxlanLoopback, 24), vxlanLoopback, switchMac);
        arpIntercept(vxlanLoopback, deviceId);
        icmpIntercept(vxlanLoopback, deviceId);


        VxlanTunnelId tunnelId = new VxlanTunnelId(deviceId, vxlanDstIp, vni);
        Ip4Address nextHopIp = Ip4Address.valueOf("10.64.11.254");
        MacAddress nextHopMac = processor.getMac(nextHopIp, tunnelId);



        //RG server to AWS
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchInPort(rgServer);


        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.extension(new NoviflowSetVxLan(switchMac,nextHopMac, vxlanLoopback, vxlanDstIp, 10384, vni), deviceId);
        treatment.setOutput(awsUplink);

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(ENCAPSULATION_PRIORITY);

        rule.forTable(0);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        flowRuleService.applyFlowRules(rule.build());

        //AWS to RG server

        selector = DefaultTrafficSelector.builder();
        selector.matchInPort(awsUplink);
        selector.matchIPSrc(vxlanDstIp.toIpPrefix());


        treatment = DefaultTrafficTreatment.builder();
        treatment.extension(new NoviflowPopVxLan(), deviceId);
        treatment.setOutput(rgServer);

        rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(DECAPSULATION_PRIORITY);
        rule.forTable(0);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        flowRuleService.applyFlowRules(rule.build());

        log.info("Cloud shithook applied");


    }


    private class NoviAggSwitchConfigListener implements NetworkConfigListener {

        private Logger log = LoggerFactory.getLogger(getClass());

        private final ConfigFactory<ApplicationId, NoviAggSwitchConfig> cfgFactory =
                new ConfigFactory<ApplicationId, NoviAggSwitchConfig>(SubjectFactories.APP_SUBJECT_FACTORY,
                        NoviAggSwitchConfig.class, "noviaggswitch") {

                    @Override
                    public NoviAggSwitchConfig createConfig() {
                        return new NoviAggSwitchConfig();
                    }
                };

        @Override
        public void event(NetworkConfigEvent event){

            log.info(event.toString());


            if (event.configClass().equals(NoviAggSwitchConfig.class)) {

                switch(event.type()){
                    case CONFIG_REGISTERED:
                        log.info("NoviAggSwitch Config registered");
                        break;
                    case CONFIG_ADDED:
                        log.info("NoviAggSwitch Config added");
                        break;
                    case CONFIG_UPDATED:
                        log.info("NoviAggSwitch config updated");
                        break;
                    case CONFIG_REMOVED:
                        log.info("NoviAggSwitch Config removed");
                        break;
                    default:
                        log.info("NoviAggSwitch config, unexpected action : "  + event.type());
                        return;
                }
                log.info("Applying new config");

                //NoviAggSwitchComponent.getComponent().newConfig((NoviAggSwitchConfig) event.config().get(), (NoviAggSwitchConfig) event.prevConfig().get());
            } else {

                log.info("Config event " + event.configClass() + " != " + NoviAggSwitchConfig.class);

            }

        }


        public ConfigFactory<ApplicationId, NoviAggSwitchConfig> getCfgAppFactory() {
            return cfgFactory;
        }


    }



}


