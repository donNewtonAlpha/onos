
package org.onosproject.phase1;

import org.apache.felix.scr.annotations.*;



import org.onlab.packet.*;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.apps.TorService;
import org.onosproject.net.flow.*;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.*;
import org.onosproject.net.packet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.*;


import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;


/**
 * Created by nick on 6/26/15.
 */
@Component(immediate = true)
@Service
public class Phase1Component{


    static final Logger log = LoggerFactory.getLogger(Phase1Component.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;


    static ApplicationId appId;
    static final DeviceId torId = DeviceId.deviceId("of:000000000000da7a");

    static final int ACL_TABLE = 60;
    static final int PORT_TABLE = 0;
    static final int VLAN_TABLE = 10;
    static final int TMAC_TABLE = 20;
    static final int VLAN1_TABLE = 11;
    static final int BRIDGING_TABLE = 50;

    static final int IP_LEARNING_LENGTH = 50;



    // Physical port 1, port splitting

    static final PortNumber internetPort = PortNumber.portNumber(2);






    static final VlanId internetVlan = VlanId.vlanId((short) 500);
    static final VlanId outsideNetworkVlan = VlanId.vlanId((short) 501);




    @Activate
    protected void activate() {

        log.debug("trying to activate");
        appId = coreService.registerApplication("org.onosproject.tor");

        //Creation of the network objects
        
        LinkedList<Integer> oltVlans = new LinkedList<>();
        LinkedList<Integer> serverVlans = new LinkedList<>();
        
        for(int i = 2; i < 10; i++){
            oltVlans.add(i);
            serverVlans.add(i);
        }
        Olt olt = new Olt(3, oltVlans);
        VsgServer vsgServer = new VsgServer(1, serverVlans);
   

        //Limitation for now, single tor
        //TODO: 2 tors
        
        //Connecting the 2
        connectOltToServer(olt, vsgServer, torId);

    }


    @Deactivate
    protected void deactivate() {

        flowRuleService.removeFlowRulesById(appId);


        Iterable<Group> appGroups = groupService.getGroups(torId, appId);
        for(Group group : appGroups) {
            groupService.removeGroup(group.deviceId(), group.appCookie(), group.appId());
        }

        log.info("Stopped");
    }


    private void connectOltToServer(Olt olt, VsgServer server, DeviceId device){

        List<VlanId> oltVlans = olt.getVlanHandled();
        List<VlanId> serverVlans = server.getVlanHandled();

        for(VlanId oltVlan : oltVlans){
            for(VlanId serverVlan : serverVlans){

                if(oltVlan.equals(serverVlan)){
                    //They need to be connected
                    oltToServerBidirectionnal(oltVlan,olt.getPortNumber(), server.getPortNumber(), device);
                    log.debug("Connecting vlan " + oltVlan +
                            " from " + olt.getPortNumber() + 
                            " to " + server.getPortNumber() +
                            " on device : " + device);
                }

            }
        }

    }


    private void oltToServerBidirectionnal(VlanId vlanId,PortNumber oltPort, PortNumber serverPort,  DeviceId deviceId){


        vlanTableFlows(oltPort, vlanId, deviceId);
        vlanTableFlows(serverPort, vlanId, deviceId);

        TrafficSelector.Builder OltToServerSelector = DefaultTrafficSelector.builder();
        OltToServerSelector.matchInPort(oltPort);
        OltToServerSelector.matchVlanId(vlanId);

        //ACL Table flow for olt to  server

        TrafficTreatment.Builder outputServerLan = DefaultTrafficTreatment.builder();
        outputServerLan.group(GroupFinder.getL2Interface(serverPort, vlanId, deviceId));


        FlowRule.Builder oltToServer = DefaultFlowRule.builder();
        oltToServer.withSelector(OltToServerSelector.build());
        oltToServer.withTreatment(outputServerLan.build());
        oltToServer.withPriority(41003);
        oltToServer.fromApp(appId);
        oltToServer.forTable(ACL_TABLE);
        oltToServer.makePermanent();
        oltToServer.forDevice(deviceId);

        flowRuleService.applyFlowRules(oltToServer.build());



        //Flow from the extended LAN to the OLT (ACL)


        TrafficSelector.Builder toOltSelector = DefaultTrafficSelector.builder();
        toOltSelector.matchInPort(serverPort);
        toOltSelector.matchVlanId(vlanId);

        TrafficTreatment.Builder toOltGroupTreatment = DefaultTrafficTreatment.builder();
        toOltGroupTreatment.group(GroupFinder.getL2Interface(oltPort, vlanId, deviceId));



        FlowRule.Builder serverToOlt = DefaultFlowRule.builder();
        serverToOlt.withSelector(toOltSelector.build());
        serverToOlt.withTreatment(toOltGroupTreatment.build());
        serverToOlt.withPriority(41002);
        serverToOlt.fromApp(appId);
        serverToOlt.forTable(ACL_TABLE);
        serverToOlt.makePermanent();
        serverToOlt.forDevice(deviceId);

        flowRuleService.applyFlowRules(serverToOlt.build());


    }




    private void vlanTableFlows(PortNumber port, VlanId vlanId, DeviceId deviceId){

        TrafficSelector.Builder vlanTableHackSelector = DefaultTrafficSelector.builder();
        vlanTableHackSelector.matchInPort(port);
        vlanTableHackSelector.matchVlanId(vlanId);

        TrafficTreatment.Builder vlanTableHackTreatment = DefaultTrafficTreatment.builder();
        vlanTableHackTreatment.transition(TMAC_TABLE);

        FlowRule.Builder vlanTableHackRule = DefaultFlowRule.builder();
        vlanTableHackRule.withSelector(vlanTableHackSelector.build());
        vlanTableHackRule.withTreatment(vlanTableHackTreatment.build());
        vlanTableHackRule.withPriority(8);
        vlanTableHackRule.forTable(VLAN_TABLE);
        vlanTableHackRule.fromApp(appId);
        vlanTableHackRule.forDevice(deviceId);
        vlanTableHackRule.makePermanent();

        flowRuleService.applyFlowRules(vlanTableHackRule.build());

    }





}


