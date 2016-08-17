
package org.onosproject.noviaggswitch;


import org.apache.felix.scr.annotations.*;


import org.onlab.packet.*;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.driver.extensions.NoviflowExtensionTreatmentInterpretor;
import org.onosproject.driver.extensions.NoviflowMatchVni;
import org.onosproject.driver.extensions.NoviflowPopVxLan;
import org.onosproject.driver.extensions.NoviflowSetVxLan;
import org.onosproject.driver.extensions.ofmessages.OFNoviflowVniExperimenterMsg;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.device.DeviceService;
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
import org.onosproject.noviaggswitch.config.NoviAggSwitchConfig;
import org.onosproject.noviaggswitch.config.NoviAggSwitchConfigListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Semaphore;


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


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;


    private static final int ARP_INTERCEPT_PRIORITY = 15000;
    private static final int ICMP_INTERCEPT_PRIORITY = 14000;
    private static final int IGMP_INTERCEPT_PRIORITY = 13000;
    private static final int ENCAPSULATION_PRIORITY = 1600;
    private static final int DECAPSULATION_PRIORITY = 1000;


    static ApplicationId appId;


    static final DeviceId deviceId = DeviceId.deviceId("of:000000223d5a00d9");

    private static MacAddress switchMac = MacAddress.valueOf("68:05:33:44:55:66");
    private static Ip4Address aggSwitchIP = Ip4Address.valueOf("10.50.1.1");
    private static Ip4Address primaryLinkIP = Ip4Address.valueOf("10.20.1.1");
    private static Ip4Address secondaryLinlkIP = Ip4Address.valueOf("10.20.2.1");
    private PortNumber bngPort =  PortNumber.portNumber(1);
    private PortNumber secondaryBngPort = PortNumber.portNumber(2);


    private NoviAggSwitchPacketProcessor processor;
    private LinkFailureDetection linkFailureDetection;
    private NoviAggSwitchConfigListener cfgListener;

    private HashMap<DeviceId, MulticastHandler> multicastHandlers;


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




        /*

        //Config
        cfgListener = new NoviAggSwitchConfigListener();
        cfgService.registerConfigFactory(cfgListener.getCfgAppFactory());
        cfgService.addListener(cfgListener);


        */
        

        //Packet processor
        processor = new NoviAggSwitchPacketProcessor(packetService);
        packetService.addProcessor(processor, 1);
        
        //Routing info
        //4 info

        //loopback
        processor.addRoutingInfo(deviceId, bngPort, Ip4Prefix.valueOf(aggSwitchIP, 24), aggSwitchIP, MacAddress.valueOf("00:00:00:00:00:00"));
        processor.addRoutingInfo(deviceId, secondaryBngPort, Ip4Prefix.valueOf(aggSwitchIP, 24), aggSwitchIP, MacAddress.valueOf("00:00:00:00:00:00"));
        //Uplinks
        processor.addRoutingInfo(deviceId, bngPort, Ip4Prefix.valueOf(primaryLinkIP, 24), primaryLinkIP, MacAddress.valueOf("68:05:11:11:11:11"));
        processor.addRoutingInfo(deviceId, secondaryBngPort, Ip4Prefix.valueOf(secondaryLinlkIP, 24), secondaryLinlkIP, MacAddress.valueOf("68:05:22:22:22:22"));


        //LinkFailureDetection
        List<ConnectPoint> redundancyPorts = new LinkedList<>();
        redundancyPorts.add(new ConnectPoint(deviceId, PortNumber.portNumber(7)));
        redundancyPorts.add(new ConnectPoint(deviceId, PortNumber.portNumber(8)));

        linkFailureDetection = new LinkFailureDetection(flowRuleService, redundancyPorts);
        deviceService.addListener(linkFailureDetection);


        //IPs the agg switch is responding to ARP
        //arpIntercept(aggSwitchIP);
        arpIntercept(primaryLinkIP, deviceId);
        arpIntercept(secondaryLinlkIP, deviceId);

        //IPs the agg switch is responding to ping
        icmpIntercept(aggSwitchIP, deviceId);
        icmpIntercept(primaryLinkIP, deviceId);
        icmpIntercept(secondaryLinlkIP, deviceId);




        /*
        //Multicast handler

        igmpIntercept();
        addMulticastHandler(deviceId);


        */

        log.info("NoviFlow AggSwitch activated");

    }


    @Deactivate
    protected void deactivate() {

        flowRuleService.removeFlowRulesById(appId);


        Iterable<Group> appGroups = groupService.getGroups(deviceId, appId);
        for(Group group : appGroups) {
            groupService.removeGroup(group.deviceId(), group.appCookie(), group.appId());
        }

        //Clear meters
        Collection<Meter> meters = meterService.getMeters(deviceId);
        for(Meter meter : meters) {
            if(meter.appId().equals(appId)){
                meterService.withdraw(DefaultMeterRequest.builder().remove(), meter.id());
            }
        }

        packetService.removeProcessor(processor);



        log.info("Stopped");
    }

    public void addAccessDevice(int port, int vni, String bngVxlanIp) {

        addAccessDevice(port, vni, bngVxlanIp, bngVxlanIp, bngVxlanIp);

    }

    public void addAccessDevice(int port, int vni, String bngVxlanIp, String viaPrimaryIP, String viaSecondaryIP) {

        Random rand = new Random();
        int udpPort = rand.nextInt() + 2000;


        addAccessDevice(port, vni, udpPort, bngVxlanIp, viaPrimaryIP, viaSecondaryIP, aggSwitchIP.toString(), switchMac.toString());

    }

    private void addAccessDevice(int port, int vni, int udpPort, String bngVxlanIp, String viaPrimaryIP, String viaSecondaryIP, String switchVxlanIp, String switchVxlanMac) {

        Runnable r = new Runnable() {

            @Override
            public void run() {
                Ip4Address bngVxLanIP = Ip4Address.valueOf(bngVxlanIp);

                //PrimaryPath
                MacAddress bngVxLanPrimaryMac = processor.getMac(Ip4Address.valueOf(viaPrimaryIP));
                if(bngVxLanPrimaryMac != null) {
                    log.info("MAC found, ready to add flows");

                    try {
                        accessToBng(PortNumber.portNumber(port), vni, udpPort, bngVxLanIP, bngVxLanPrimaryMac, Ip4Address.valueOf(switchVxlanIp), MacAddress.valueOf(switchVxlanMac), true);
                        bngToAccess(PortNumber.portNumber(port), vni, Ip4Address.valueOf(bngVxlanIp), true);

                    } catch (Exception e) {
                        log.warn("Exception", e);
                    }

                    //SecondaryPath if different from primary

                    if (!viaSecondaryIP.equals(viaPrimaryIP)) {

                        MacAddress bngVxLanSecondaryMac = processor.getMac(Ip4Address.valueOf(viaSecondaryIP));
                        if(bngVxLanSecondaryMac != null) {
                            log.info("MAC found, ready to add flows");

                            try {
                                accessToBng(PortNumber.portNumber(port), vni, udpPort, bngVxLanIP, bngVxLanSecondaryMac, Ip4Address.valueOf(switchVxlanIp), MacAddress.valueOf(switchVxlanMac), false);
                                bngToAccess(PortNumber.portNumber(port), vni, Ip4Address.valueOf(bngVxlanIp), false);

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

/*    private void addAccessDevice(int port, int vni, int udpPort, String bngVxlanIp, String switchVxlanIp, String switchVxlanMac) {

        addAccessDevice(port, vni, udpPort, bngVxlanIp, bngVxlanIp, switchVxlanIp, switchVxlanMac);

    }*/

    private void accessToBng(PortNumber port, int vni, int udpPort, Ip4Address bngVxlanIp, MacAddress bngVxlanMac, Ip4Address switchVxlanIp, MacAddress switchVxlanMac, boolean primary){

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchInPort(port);


        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.extension(new NoviflowSetVxLan(switchVxlanMac,bngVxlanMac, switchVxlanIp, bngVxlanIp, udpPort, vni), deviceId);
        if(primary) {
            treatment.setOutput(bngPort);
        } else {
            treatment.setOutput(secondaryBngPort);
        }

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        if(primary) {
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


    private void bngToAccess(PortNumber port, int vni, Ip4Address bngVxLanIP, boolean primary) {

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        if(primary) {
            selector.matchInPort(bngPort);
        } else {
            selector.matchInPort(secondaryBngPort);
        }
        selector.matchIPSrc(bngVxLanIP.toIpPrefix());


        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.extension(new NoviflowPopVxLan(), deviceId);
        treatment.setOutput(port);

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        if(primary) {
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

    public void removeTunnel(Ip4Address vxlanIP, int vni) {

        //TODO deviceId
        List<VxLanTunnel> tunnels = getTunnels(deviceId);
        for(VxLanTunnel tunnel : tunnels) {
            if (tunnel.match(vxlanIP, vni)) {
                for (FlowRule flow : tunnel.getFlows()) {
                    flowRuleService.removeFlowRules(flow);
                }
            }
        }

    }

    public void removeAllTunnels() {

        Set<DeviceId> devices = multicastHandlers.keySet();
        for(DeviceId deviceId : devices) {
            removeAllTunnels(deviceId);
        }
    }

    public void removeAllTunnels(DeviceId deviceId) {

        Iterable<FlowRule> flows = flowRuleService.getFlowRulesById(appId);
        for (FlowRule flow : flows) {

            if(flow.deviceId().equals(deviceId)) {

                List<Instruction> instructions = flow.treatment().immediate();
                for (Instruction instruction : instructions) {

                    if (instruction.type() == Instruction.Type.EXTENSION) {

                        try {
                            Instructions.ExtensionInstructionWrapper extensionInstruction = (Instructions.ExtensionInstructionWrapper) instruction;
                            ExtensionTreatment extension = extensionInstruction.extensionInstruction();

                            if (extension.type().equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NOVIFLOW_SET_VXLAN)) {

                                flowRuleService.removeFlowRules(flow);

                            } else if (extension.type().equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NOVIFLOW_POP_VXLAN)) {

                                flowRuleService.removeFlowRules(flow);

                            }

                        } catch (Exception e) {
                            log.warn("remove tunnel exception", e);
                        }
                    }

                }
            }

        }
    }

    public List<VxLanTunnel> getTunnels(DeviceId deviceId) {

        List<VxLanTunnel> tunnels = new LinkedList<>();

        Iterable<FlowRule> flows = flowRuleService.getFlowRulesById(appId);
        for (FlowRule flow : flows) {

            if(flow.deviceId().equals(deviceId)) {

                List<Instruction> instructions = flow.treatment().immediate();
                for (Instruction instruction : instructions) {

                    if (instruction.type() == Instruction.Type.EXTENSION) {

                        try {
                            Instructions.ExtensionInstructionWrapper extensionInstruction = (Instructions.ExtensionInstructionWrapper) instruction;
                            ExtensionTreatment extension = extensionInstruction.extensionInstruction();

                            if (extension.type().equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NOVIFLOW_SET_VXLAN)) {

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
                                    tunnels.add(newTunnel);
                                }


                            } else if (extension.type().equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NOVIFLOW_POP_VXLAN)) {

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

        return tunnels;


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

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchIPProtocol(IPv4.PROTOCOL_IGMP);


        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.setOutput(bngPort);
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


    private void addMulticastHandler(DeviceId deviceId) {

        MulticastHandler multicastHandler = new MulticastHandler(deviceId, flowRuleService, groupService, appId);

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

                if(flow.selector().getCriterion(Criterion.Type.ARP_TPA) != null) {
                    //ARP intercepts
                    flowRuleService.removeFlowRules(flow);
                } else if (flow.selector().getCriterion(Criterion.Type.IP_PROTO) != null){
                    //ICMP or IGMP intercepts
                    flowRuleService.removeFlowRules(flow);
                }
            }

        }
    }

    public void newConfig(NoviAggSwitchConfig config) {

        if(config == null) {
         log.warn("Null config, don't know what to do so do nothing");
         return;
        }

        if(!config.isValid()) {
         log.warn("Invalid config, don't know what to do so do nothing");
        }

        //Clean up old knowledge
        clearIntercepts(config.deviceId());
        //TODO : multi switch ------------
        processor.clearMacRequests();
        processor.clearRoutingInfo();
        // -------------------------------
        getMulticastHandler(config.deviceId()).kill();
        removeMulticastHandler(config.deviceId());

        List<VxLanTunnel> tunnels = getTunnels(config.deviceId());
        removeAllTunnels(config.deviceId());

        linkFailureDetection.removeDevice(config.deviceId());

        //New Knowledge

        //IPs the agg switch is responding to ARP
        arpIntercept(config.primaryLinkIp(), config.deviceId());
        arpIntercept(config.secondaryLinkIp(), config.deviceId());

        //IPs the agg switch is responding to ping
        icmpIntercept(config.loopbackIp(), config.deviceId());
        icmpIntercept(config.primaryLinkIp(), config.deviceId());
        icmpIntercept(config.secondaryLinkIp(), config.deviceId());

        //loopback
        processor.addRoutingInfo(config.deviceId(), config.primaryLinkPort(), Ip4Prefix.valueOf(config.loopbackIp(), 24), config.loopbackIp(), MacAddress.valueOf("00:00:00:00:00:00"));
        processor.addRoutingInfo(config.deviceId(), config.secondaryLinkPort(), Ip4Prefix.valueOf(config.loopbackIp(), 24), config.loopbackIp(), MacAddress.valueOf("00:00:00:00:00:00"));
        //Uplinks
        processor.addRoutingInfo(config.deviceId(), config.primaryLinkPort(), Ip4Prefix.valueOf(config.primaryLinkIp(), 31), config.primaryLinkIp(), config.primaryLinkMac());
        processor.addRoutingInfo(config.deviceId(), config.secondaryLinkPort(), Ip4Prefix.valueOf(config.secondaryLinkIp(), 31), config.secondaryLinkIp(), config.secondaryLinkMac());


        igmpIntercept(config.deviceId());
        addMulticastHandler(config.deviceId());

        //LinkFailureDetection

        linkFailureDetection.addRedundancyPort(new ConnectPoint(config.deviceId(), config.primaryLinkPort()));
        linkFailureDetection.addRedundancyPort(new ConnectPoint(config.deviceId(), config.secondaryLinkPort()));


        //Reinstate tunnels

        Random rand = new Random();


        for(VxLanTunnel tunnel : tunnels) {

            int udpPort = rand.nextInt() + 2000;

            accessToBng(tunnel.getPort(), tunnel.getVni(), udpPort, tunnel.getDstIp(), tunnel.getPrimaryViaMac(), config.loopbackIp(), config.primaryLinkMac(), true);
            bngToAccess(tunnel.getPort(), tunnel.getVni(), tunnel.getDstIp(), true);
            accessToBng(tunnel.getPort(), tunnel.getVni(), udpPort, tunnel.getDstIp(), tunnel.getSecondaryViaMac(), config.loopbackIp(), config.secondaryLinkMac(), false);
            bngToAccess(tunnel.getPort(), tunnel.getVni(), tunnel.getDstIp(), false);

        }

    }

    /*private void noviExpSetup() {

        Runnable r = new Runnable() {
            @Override
            public void run() {

                OFNoviflowVniExperimenterMsg msg = new OFNoviflowVniExperimenterMsg(0);

                try {
                    byte[] ip = new byte[4];
                    ip[0] = 10;
                    ip[1] = 64;
                    ip[2] = 1;
                    ip[3] = 85;
                    InetAddress dstIp = InetAddress.getByAddress(ip);
                    ip[3] = 50;
                    InetAddress sourceIp = InetAddress.getByAddress(ip);
                    log.info("Source : " + sourceIp);
                    log.info("Destination : " + dstIp);
                    Socket socket = new Socket(dstIp, 40095, sourceIp, 6653);
                    OutputStream os = socket.getOutputStream();

                    InputStream is = socket.getInputStream();

                    while (true) {
                        os.write(msg.fullIPPayload());
                        Thread.sleep(1000);

                        if (is.available() > 0) {
                            byte[] response = new byte[is.available()];
                            is.read(response);
                            //send to log

                            StringBuilder builder = new StringBuilder("Response : ");
                            for (int i = 0; i < response.length; i++) {
                                builder.append(response[i]);
                                builder.append(' ');
                            }
                            log.info(builder.toString());

                        }
                    }


                } catch (Exception e) {
                    log.error("Exception noviExpSetup", e);
                }
            }
        };

        Thread t =new Thread(r);
        t.setDaemon(true);
        t.start();

    }*/



}


