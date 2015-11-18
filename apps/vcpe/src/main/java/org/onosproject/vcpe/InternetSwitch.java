package org.onosproject.vcpe;

import org.onlab.packet.*;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.*;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by nick on 7/10/15.
 */
public class InternetSwitch {

    private static ApplicationId appId;
    private static FlowRuleService flowRuleService;

    static final Logger log = LoggerFactory.getLogger(InternetSwitch.class);
    //devices and ports initialization
    static DeviceId internetSwitch;
    static PortNumber internetPort = PortNumber.portNumber(2);
    static PortNumber dhcpPort = PortNumber.portNumber(1);


    private static HashMap<Integer, FlowRule> fromInternetFlows = new HashMap<>();
    private static HashMap<Integer, FlowRule> toInternetFlows = new HashMap<>();
    private static HashMap<Integer, FlowRule> toControllerInternetFlows = new HashMap<>();
    private static List<Integer> enabledCustomers = new LinkedList<>();

    public InternetSwitch(){}

    InternetSwitch(ApplicationId appId, FlowRuleService flowRuleService, DeviceId deviceId){
        this.appId = appId;
        this.flowRuleService = flowRuleService;
        internetSwitch = deviceId;

    }

    void initiate() {


        // Broadcasting ARP messages flow
        TrafficSelector.Builder arpInternetSelector = DefaultTrafficSelector.builder();
        arpInternetSelector.matchEthType(Ethernet.TYPE_ARP);

        TrafficTreatment.Builder arpInternetTreatment = DefaultTrafficTreatment.builder();
        arpInternetTreatment.setOutput(PortNumber.FLOOD);


        FlowRule.Builder arpInternetFlow = DefaultFlowRule.builder();
        arpInternetFlow.forDevice(internetSwitch);
        arpInternetFlow.fromApp(appId);
        arpInternetFlow.withPriority(42000);
        arpInternetFlow.makePermanent();
        arpInternetFlow.withSelector(arpInternetSelector.build());
        arpInternetFlow.withTreatment(arpInternetTreatment.build());

        flowRuleService.applyFlowRules(arpInternetFlow.build());

        //DHCP flows
        //To the DHCP service: request
        TrafficSelector.Builder toDhcpSelector = DefaultTrafficSelector.builder();
        toDhcpSelector.matchEthType(Ethernet.TYPE_IPV4);
        toDhcpSelector.matchUdpDst(TpPort.tpPort(67));
        toDhcpSelector.matchIPProtocol((byte) 17);

        TrafficTreatment.Builder toDhcpTreatment = DefaultTrafficTreatment.builder();
        toDhcpTreatment.setOutput(dhcpPort);

        FlowRule.Builder toDhcpFlow = DefaultFlowRule.builder();
        toDhcpFlow.forDevice(internetSwitch);
        toDhcpFlow.fromApp(appId);
        toDhcpFlow.makePermanent();
        toDhcpFlow.withPriority(49000);
        toDhcpFlow.withSelector(toDhcpSelector.build());
        toDhcpFlow.withTreatment(toDhcpTreatment.build());

        //DHCP response
        TrafficSelector.Builder fromDhcpSelector = DefaultTrafficSelector.builder();
        fromDhcpSelector.matchEthType(Ethernet.TYPE_IPV4);
        fromDhcpSelector.matchIPProtocol((byte) 17);
        fromDhcpSelector.matchUdpDst(TpPort.tpPort(68));

        TrafficTreatment.Builder fromDhcpTreatment = DefaultTrafficTreatment.builder();
        fromDhcpTreatment.setOutput(PortNumber.FLOOD);

        FlowRule.Builder fromDhcpFlow = DefaultFlowRule.builder();
        fromDhcpFlow.forDevice(internetSwitch);
        fromDhcpFlow.fromApp(appId);
        fromDhcpFlow.makePermanent();
        fromDhcpFlow.withPriority(49000);
        fromDhcpFlow.withSelector(fromDhcpSelector.build());
        fromDhcpFlow.withTreatment(fromDhcpTreatment.build());

        flowRuleService.applyFlowRules(toDhcpFlow.build(), fromDhcpFlow.build());


        log.info("Internet Switch initiated");
    }

    public static PortNumber getCustomerSideInternetPort(int clientId){
        return PortNumber.portNumber(clientId);
    }

    public static int getClientIdFromPortNumber(PortNumber portNumber){
        return (int) (portNumber.toLong());

    }

    void connectCustomer(int clientId){
        log.info("connect customer function");
        // check if the customer is already connected
        FlowRule flow = toInternetFlows.get(clientId);
        if (flow != null) {
            log.info("customer already connected");
            return;
        }
        enabledCustomers.add(clientId);
        log.info("client : " + clientId + "added to the enabledCustomer list");

        //The customer is not connected, creation of a selector to request packets send by this customer
        //We will use it to learn his VCPE's IP address (public IP) and create the appropriate intent


        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchInPort(getCustomerSideInternetPort(clientId));

        FlowRule.Builder connectionFlow = DefaultFlowRule.builder();
        connectionFlow.fromApp(appId);
        connectionFlow.withSelector(selector.build());
        connectionFlow.forDevice(internetSwitch);
        connectionFlow.withPriority(41000);
        connectionFlow.makePermanent();
        connectionFlow.withTreatment(DefaultTrafficTreatment.builder().punt().build());

        FlowRule toControllerInternetFlow = connectionFlow.build();
        toControllerInternetFlows.put(clientId, toControllerInternetFlow);
        log.info("temp flow internet added, clientId : "+clientId);

        flowRuleService.applyFlowRules(toControllerInternetFlow);
    }

    public void disconnectCustomer(int clientId) {

        log.info("disconnect customer function, client : " + clientId);
        FlowRule flowToInternet = toInternetFlows.get(clientId);
        FlowRule flowFromInternet = fromInternetFlows.get(clientId);
        log.info("ready to remove flows");

        fromInternetFlows.remove(clientId);
        toInternetFlows.remove(clientId);

        if(flowFromInternet != null){
            flowRuleService.removeFlowRules(flowFromInternet);
        }
        if(flowToInternet != null){
            flowRuleService.removeFlowRules(flowToInternet);
        }

        enabledCustomers.remove(new Integer(clientId));
        log.info("flows removed");

    }

    void processPacket(PacketContext context){

        InboundPacket pkt = context.inPacket();
        Ethernet ethPkt = pkt.parsed();
        IPacket payload = ethPkt.getPayload();

        int clientId = getClientIdFromPortNumber(pkt.receivedFrom().port());

        boolean b =  true;
        IpAddress ip = null;

        if(b) {
            try {
                ARP arpPacket = (ARP) payload;
                ip = Ip4Address.valueOf(arpPacket.getSenderProtocolAddress());
                b = false;
            } catch (Exception e) {
                log.warn("Problem : it may not be an ARP packet", e);
            }
        }
        if(b){
            try {
                IPv4 ip4Packet = (IPv4) payload;
                log.info("It is a IPv4 packet, ip int : " +ip4Packet.getSourceAddress());
                ip = IpAddress.valueOf(ip4Packet.getSourceAddress());
                b = false;
            } catch (Exception e) {
                if(b){
                    log.error("Problem : it is not an ARP packet nor a IPv4 packet", e);
                    return;
                }
            }
        }

        log.info("customer ip : " + ip);

        FlowRule toControllerInternetFlow = toControllerInternetFlows.remove(clientId);
        log.info("trying to remove the temporary flow");
        if(toControllerInternetFlow != null){
            log.info("removing the temporary flow");
            flowRuleService.removeFlowRules(toControllerInternetFlow);
        }

        if(!enabledCustomers.contains(clientId)){
            log.info("this customer is not enabled");
            return;
        }

        //Treatment of this packet
        context.treatmentBuilder().setOutput(internetPort);
        context.send();

        //Flow from the customer's VCPE to the internet
        TrafficSelector.Builder toInternetSelector = DefaultTrafficSelector.builder();
        toInternetSelector.matchInPort(getCustomerSideInternetPort(clientId));

        TrafficTreatment.Builder toInternetTreatment = DefaultTrafficTreatment.builder();
        toInternetTreatment.setOutput(internetPort);

        FlowRule.Builder toInternetFlowBuilder = DefaultFlowRule.builder();
        toInternetFlowBuilder.forDevice(internetSwitch);
        toInternetFlowBuilder.fromApp(appId);
        toInternetFlowBuilder.withPriority(45001);
        toInternetFlowBuilder.makePermanent();
        toInternetFlowBuilder.withSelector(toInternetSelector.build());
        toInternetFlowBuilder.withTreatment(toInternetTreatment.build());

        FlowRule toInternetFlow = toInternetFlowBuilder.build();
        toInternetFlows.put(clientId, toInternetFlow);


        //Flow from the Internet to the customer's VCPE
        TrafficSelector.Builder fromInternetSelector = DefaultTrafficSelector.builder();
        fromInternetSelector.matchIPDst(ip.toIpPrefix());
        fromInternetSelector.matchEthType(Ethernet.TYPE_IPV4);

        TrafficTreatment.Builder fromInternetTreatment = DefaultTrafficTreatment.builder();
        fromInternetTreatment.setOutput(getCustomerSideInternetPort(clientId));

        FlowRule.Builder fromInternetFlowBuilder = DefaultFlowRule.builder();
        fromInternetFlowBuilder.forDevice(internetSwitch);
        fromInternetFlowBuilder.fromApp(appId);
        fromInternetFlowBuilder.withPriority(45000);
        fromInternetFlowBuilder.makePermanent();
        fromInternetFlowBuilder.withSelector(fromInternetSelector.build());
        fromInternetFlowBuilder.withTreatment(fromInternetTreatment.build());

        FlowRule fromInternetFlow = fromInternetFlowBuilder.build();
        fromInternetFlows.put(clientId, fromInternetFlow);


        flowRuleService.applyFlowRules(toInternetFlow, fromInternetFlow);

        log.info("flows added");

    }
}
