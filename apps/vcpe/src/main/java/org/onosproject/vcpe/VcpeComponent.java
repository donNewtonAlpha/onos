package org.onosproject.vcpe;

import org.apache.felix.scr.annotations.*;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPacket;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.*;
import org.onosproject.net.packet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.*;
import org.glassfish.json.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;


/**
 * Created by nick on 6/26/15.
 */
@Component(immediate = true)

public class VcpeComponent {


    static final Logger log = LoggerFactory.getLogger(VcpeComponent.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;


    private static ApplicationId appId;
    private static final int JSON_COMMUNICATION_PORT = 6493;

    //devices and ports initialization
    private static  DeviceId internetSwitchDeviceId = DeviceId.deviceId("of:0000b8ca3a65f363");
    private static  DeviceId lanSwitchDeviceId = DeviceId.deviceId("of:0000a0369f2788f0");



    private static final String CONNECT = "connectInternet";
    private static final String DISCONNECT = "disconnectInternet";
    private static final String GENERAL_DNS = "generalDns";
    private static final String PERSONAL_DNS = "personalDns";
    private static final String NO_DNS_SERVICE = "noDns";
    private static final String CONNECT_TV = "connectUverse";
    private static final String DISCONNECT_TV = "disconnectUverse";
    private static final String INITIATE_CUSTOMER = "initiateCustomer";


    private InternetSwitch internetSwitch;
    private LanSwitch lanSwitch;

    private PacketProcessor processor;

    @Activate
    protected void activate() {

        log.debug("trying to activate");
        appId = coreService.registerApplication("org.onosproject.vcpe");

        internetSwitch = new InternetSwitch(appId, flowRuleService, internetSwitchDeviceId);
        internetSwitch.initiate();;
        lanSwitch = new LanSwitch(appId, flowRuleService,packetService, lanSwitchDeviceId);
        lanSwitch.initiate();

        VcpeServiceImplementation.setSwitches(internetSwitch, lanSwitch);
        DnsProxy.initiate();

        listen();

        // custom processor to learn the customer's VCPE IP address and apply DNs protection
        processor = new VcpePacketProcessor();
        packetService.addProcessor(processor, PacketProcessor.ADVISOR_MAX + 2);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {

        Iterable<FlowRule> appFlows = flowRuleService.getFlowRulesById(appId);
        for(FlowRule flow : appFlows) {
            flowRuleService.removeFlowRules(flow);
        }
        //remove packet processor ?

        log.info("Stopped");
    }



    public void listen() {


        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket(JSON_COMMUNICATION_PORT);
                    while (true) {
                        Socket socket = serverSocket.accept();
                        treatJson(socket);
                    }
                }catch(IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        };

        Thread listenningThread = new Thread(r);
        listenningThread.setDaemon(true);
        listenningThread.start();
        log.info("Thread listenning for the web interface");

    }






    private void applyAction(String action, int clientId){
        log.info("applyAction function, action : " + action +", clientId : "+clientId);

        switch (action){
            case CONNECT:
                internetSwitch.connectCustomer(clientId);
                break;
            case DISCONNECT:
                internetSwitch.disconnectCustomer(clientId);
                break;
            case GENERAL_DNS:
                lanSwitch.generalDns(clientId);
                break;
            case PERSONAL_DNS:
                lanSwitch.personalDns(clientId);
                break;
            case NO_DNS_SERVICE:
                lanSwitch.noDns(clientId);
                break;
            case CONNECT_TV:
                lanSwitch.connectCustomer(clientId);
                break;
            case DISCONNECT_TV:
                lanSwitch.disconnectCustomer(clientId);
                break;
            case INITIATE_CUSTOMER:
                lanSwitch.initiateCustomer(clientId);
                break;
            default:
                log.error("This message does not match any control message");
        }
    }


    private void treatJson(Socket socket){

        log.info("treatJson function");

        Runnable r = new Runnable() {
            @Override
            public void run() {
                try{
                    InputStream is = socket.getInputStream();
                    JsonReader reader = Json.createReader(is);
                    JsonObject message = reader.readObject();
                    is.close();
                    reader.close();

                    log.info("Json received : " + message.toString() );

                    int clientId = Integer.valueOf(message.getJsonString("clientId").getString());
                    String action = message.getJsonString("action").getString();

                    applyAction(action, clientId);

                }catch(Exception e){
                    log.error("treatment of JSON exception", e);
                }
            }
        };

        Thread thread = new Thread(r);
        thread.setDaemon(true);
        thread.start();

    }


    public class VcpePacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {

            if(context.isHandled()){
                return;
            }
            InboundPacket pkt = context.inPacket();


            ConnectPoint incomingPoint = pkt.receivedFrom();
            DeviceId incomingDevice = incomingPoint.deviceId();

            if(incomingDevice.equals(lanSwitchDeviceId)){
                lanSwitch.processPacket(context);
            }
            else if(incomingDevice.equals(internetSwitchDeviceId)){
                internetSwitch.processPacket(context);
            }

        }

    }

}


