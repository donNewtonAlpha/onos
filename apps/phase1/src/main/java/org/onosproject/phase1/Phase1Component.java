
package org.onosproject.phase1;

import org.apache.felix.scr.annotations.*;



import org.onlab.packet.*;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.GroupId;
import org.onosproject.net.Device;
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

public class Phase1Component{


    static final Logger log = LoggerFactory.getLogger(Phase1Component.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;


    static ApplicationId appId;
    static final DeviceId torId = DeviceId.deviceId("of:0000000000000111");




    @Activate
    protected void activate() {

        log.debug("trying to activate");
        appId = coreService.registerApplication("org.onosproject.phase1");

        //Initiation

        GroupFinder.initiate(appId, groupService);

        //OLT Test

        NetworkElements elements = new NetworkElements(flowRuleService, groupService, appId, torId);
        elements.twoWayVlanFlow(VlanId.vlanId((short)5), PortNumber.portNumber(1), PortNumber.portNumber(11), torId,41000, false );


        //Creation of the network objects
        
       /* LinkedList<Integer> oltVlans = new LinkedList<>();
        LinkedList<Integer> vm1Vlans = new LinkedList<>();
        LinkedList<Integer> vm2Vlans = new LinkedList<>();


        for(int i = 2; i < 6; i++){
            oltVlans.add(i);
            vm1Vlans.add(i);
            vm2Vlans.add(i +5);
        }
        Olt olt = new Olt(145, oltVlans);
        VsgVm vm1 = new VsgVm(Ip4Address.valueOf("10.255.255.2"), MacAddress.valueOf("52:54:00:E5:28:CF"),Ip4Prefix.valueOf("29.29.0.0/24"),vm1Vlans);
        LinkedList<VsgVm> vms1 = new LinkedList<>();
        vms1.add(vm1);

        //VsgVm vm2 = new VsgVm(Ip4Address.valueOf("10.255.255.2"), MacAddress.valueOf("14:55:00:E5:28:52"),Ip4Prefix.valueOf("29.29.0.0/24"),vm1Vlans);
        LinkedList<VsgVm> vms2 = new LinkedList<>();
        //vms1.add(vm2);


        VsgServer vsgServer1 = new VsgServer(2,MacAddress.valueOf("e4:1d:2d:08:4b:80"), vms1, Ip4Prefix.valueOf("29.29.0.0/22"));

        VsgServer vsgServer2 = new VsgServer(1,MacAddress.valueOf("e4:1d:2d:2d:ad:50"), vms2, Ip4Prefix.valueOf("29.29.43.0/22"));

        log.debug("Olt and server objects created");
        log.debug(" Olt : " + olt.toString());
        log.debug("VsgServer 1 : " + vsgServer1.toString());


        QuaggaInstance primaryQuaggaInstance = new QuaggaInstance(4, MacAddress.valueOf("52:54:00:9e:a9:8a"), true);
        QuaggaInstance secondaryQuaggaInstance = new QuaggaInstance(4, MacAddress.valueOf("52:54:00:9e:a9:8a"), false);


        NetworkElements elements = new NetworkElements(flowRuleService, groupService, appId, torId);
        elements.addElement(olt);
        elements.addElement(vsgServer1);
        elements.addElement(vsgServer2);
        elements.addElement(primaryQuaggaInstance);
        elements.addElement(secondaryQuaggaInstance);

        elements.update();*/

        //Limitation for now, single tor
        //TODO: 2 tors


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








}


