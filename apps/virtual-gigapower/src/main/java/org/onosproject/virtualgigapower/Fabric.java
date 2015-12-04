package org.onosproject.virtualgigapower;

import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.*;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by nick on 11/25/15.
 */
public class Fabric {

    private static final int MPLS_TABLE = 20;
    private static final int ACL_TABLE = 60;
    private static final int VLAN_TABLE = 10;

    private static Fabric instance;
    private List<Spine> spines;
    private List<PairOfTor> pairsOfTor;
    //private List<Leaf> otherLeaves;

    private int currentMplsLabel;


    private Fabric(){
        spines = new LinkedList<>();
        pairsOfTor = new LinkedList<>();
        //otherLeaves = new LinkedList<>();
        currentMplsLabel = 50;
    }

    public static Fabric getFabric(){
        if (instance == null){
            instance = new Fabric();
        }
        return instance;
    }

    private List<Leaf> getAllLeaves(){

        List<Leaf> leaves = new LinkedList<>();

        for(PairOfTor pair: pairsOfTor){
            leaves.addAll(pair.getLeaves());
        }
        //leaves.addAll(otherLeaves);

        return leaves;
    }

    private synchronized int getNextMplsLabel(){
        return this.currentMplsLabel ++;
    }

    public Spine addSpine(DeviceId spineId, Ip4Address managementIp, int id, String model){

        Spine newSpine = new Spine(spineId, managementIp, id, model, 32);
        spines.add(newSpine);
        return  newSpine;

    }

    public void addSpine(Spine newSpine){
        spines.add(newSpine);
    }

/*    public Leaf addLeaf(DeviceId spineId, Ip4Address  managementIp, int id, String model){
        Leaf newLeaf = new Leaf(spineId, managementIp, id, model);
        newLeaf.setMPLS(getNextMplsLabel());
        otherLeaves.add(newLeaf);
        return  newLeaf;
    }

    public  void addLeaf(Leaf newLeaf){
        newLeaf.setMPLS(getNextMplsLabel());
        otherLeaves.add(newLeaf);
    }*/

    public void addPairOfTor(Leaf leaf1, Leaf leaf2){
        PairOfTor pair = new PairOfTor(leaf1, leaf2, getNextMplsLabel());
        leaf1.setMpls(getNextMplsLabel());
        leaf2.setMpls(getNextMplsLabel());
        pairsOfTor.add(pair);
    }

    public void connectFabric(){
        spinesMplsFlows();
        applyLeavesBehaviors();
    }

    public void spinesMplsFlows(){
        //TODO: Make sure the syntax is the flows fits the switches used

        //All Spines are treated equally
        for(Spine spine: spines){
            //flows to a pair of Tor
            for(PairOfTor pair: pairsOfTor){
                // create an ECMP group to either one of the leaf
                //popMPLSlabel
                //TODO

                GroupId ecmpGroupId = null;

                TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
                selector.matchMplsLabel(MplsLabel.mplsLabel(pair.getPairMplsLabel()));

                TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
                treatment.group(ecmpGroupId);

                FlowRule.Builder rule = DefaultFlowRule.builder();
                rule.withSelector(selector.build());
                rule.withTreatment(treatment.build());
                rule.withPriority(41000);
                rule.makePermanent();
                rule.fromApp(VirtualGigaPowerComponent.appId);
                rule.forTable(MPLS_TABLE);
                rule.forDevice(spine.getDeviceId());

                VirtualGigaPowerComponent.flowRuleService.applyFlowRules(rule.build());

            }

            for(Leaf leaf: this.getAllLeaves()){
                //Create group for this specific leaf

                GroupId groupId = null;

                TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
                selector.matchMplsLabel(MplsLabel.mplsLabel(leaf.getMplsLabel()));

                TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
                treatment.group(groupId);

                FlowRule.Builder rule = DefaultFlowRule.builder();
                rule.withSelector(selector.build());
                rule.withTreatment(treatment.build());
                rule.withPriority(42000);
                rule.makePermanent();
                rule.fromApp(VirtualGigaPowerComponent.appId);
                rule.forTable(MPLS_TABLE);
                rule.forDevice(spine.getDeviceId());

                VirtualGigaPowerComponent.flowRuleService.applyFlowRules(rule.build());

                //Flow to the Internet (7750)

                TrafficSelector.Builder internetSelector = DefaultTrafficSelector.builder();
                internetSelector.matchMplsLabel(MplsLabel.mplsLabel(VirtualGigaPowerComponent.INTERNET_MPLS_LABEL));

                if(leaf.hasInternetLink()){

                    //Create output group
                    List<PortNumber> internetPorts = leaf.getInternetPorts();
                    GroupId internetGroupId = null;

                    TrafficTreatment.Builder internetTreatment = DefaultTrafficTreatment.builder();
                    internetTreatment.group(internetGroupId);


                    FlowRule.Builder internetRule = DefaultFlowRule.builder();
                    internetRule.withSelector(internetSelector.build());
                    internetRule.withTreatment(internetTreatment.build());
                    internetRule.withPriority(43000);
                    internetRule.makePermanent();
                    internetRule.fromApp(VirtualGigaPowerComponent.appId);
                    internetRule.forTable(MPLS_TABLE);
                    internetRule.forDevice(spine.getDeviceId());


                }

            }

            //Flows to the 7750 fro  the spines



            TrafficSelector.Builder internetSelector = DefaultTrafficSelector.builder();
            internetSelector.matchMplsLabel(MplsLabel.mplsLabel(VirtualGigaPowerComponent.INTERNET_MPLS_LABEL));

            if(spine.hasInternetLink()){

                //Create output group
                List<PortNumber> internetPorts = spine.getInternetPorts();
                GroupId internetGroupId = null;

                TrafficTreatment.Builder internetTreatment = DefaultTrafficTreatment.builder();
                internetTreatment.group(internetGroupId);


                FlowRule.Builder internetRule = DefaultFlowRule.builder();
                internetRule.withSelector(internetSelector.build());
                internetRule.withTreatment(internetTreatment.build());
                internetRule.withPriority(44000);
                internetRule.makePermanent();
                internetRule.fromApp(VirtualGigaPowerComponent.appId);
                internetRule.forTable(MPLS_TABLE);
                internetRule.forDevice(spine.getDeviceId());


            }

        }

    }

    public void applyLeavesBehaviors(){

        for(PairOfTor pair: pairsOfTor){
            List<SwitchingBehavior> behaviors = pair.getBehaviors();

            for(SwitchingBehavior behavior: behaviors){
                if(!behavior.isHandled()){
                    if(behavior instanceof OltBehavior){
                        OltBehavior olt = (OltBehavior) behavior;
                        //Connect this Olt to the proper server
                        if(pair.containsServer(SwitchingBehavior.oltVlanToServerVlan(olt.getSTag()))){
                            CustomerServerBehavior server = pair.getServer(olt.getSTag());

                            for(Leaf leaf: pair.getLeaves()) {
                                //Local connection
                                //Change the vlan then forward to the proper port
                                //Olt to Server

                                TrafficSelector.Builder vlanSelector = DefaultTrafficSelector.builder();
                                vlanSelector.matchInPort(olt.getPort());
                                vlanSelector.matchVlanId(VlanId.vlanId((short) olt.getSTag()));

                                TrafficTreatment.Builder vlanTreatment = DefaultTrafficTreatment.builder();
                                vlanTreatment.setVlanId(VlanId.vlanId((short) server.getSTag()));
                                vlanTreatment.transition(ACL_TABLE);

                                FlowRule.Builder vlanFlow = DefaultFlowRule.builder();
                                vlanFlow.withSelector(vlanSelector.build());
                                vlanFlow.withTreatment(vlanTreatment.build());
                                vlanFlow.withPriority(10);
                                vlanFlow.makePermanent();
                                vlanFlow.fromApp(VirtualGigaPowerComponent.appId);
                                vlanFlow.forTable(VLAN_TABLE);
                                vlanFlow.forDevice(leaf.getDeviceId());

                                VirtualGigaPowerComponent.flowRuleService.applyFlowRules(vlanFlow.build());

                                TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
                                selector.matchInPort(olt.getPort());
                                selector.matchVlanId(VlanId.vlanId((short)server.getSTag()));

                                TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
                                treatment.group(server.getGroupId(leaf));

                                FlowRule.Builder aclRule = DefaultFlowRule.builder();
                                aclRule.withSelector(selector.build());
                                aclRule.withTreatment(treatment.build());
                                aclRule.withPriority(42001);
                                aclRule.makePermanent();
                                aclRule.fromApp(VirtualGigaPowerComponent.appId);
                                aclRule.forTable(ACL_TABLE);
                                aclRule.forDevice(leaf.getDeviceId());

                                VirtualGigaPowerComponent.flowRuleService.applyFlowRules(aclRule.build());

                                //Server to Olt

                                TrafficSelector.Builder serverVlanSelector = DefaultTrafficSelector.builder();
                                serverVlanSelector.matchInPort(server.getPort());
                                serverVlanSelector.matchVlanId(VlanId.vlanId((short) server.getSTag()));

                                TrafficTreatment.Builder ServerVlanTreatment = DefaultTrafficTreatment.builder();
                                ServerVlanTreatment.setVlanId(VlanId.vlanId((short) olt.getSTag()));
                                ServerVlanTreatment.transition(ACL_TABLE);

                                FlowRule.Builder serverVlanFlow = DefaultFlowRule.builder();
                                serverVlanFlow.withSelector(serverVlanSelector.build());
                                serverVlanFlow.withTreatment(ServerVlanTreatment.build());
                                serverVlanFlow.withPriority(11);
                                serverVlanFlow.makePermanent();
                                serverVlanFlow.fromApp(VirtualGigaPowerComponent.appId);
                                serverVlanFlow.forTable(VLAN_TABLE);
                                serverVlanFlow.forDevice(leaf.getDeviceId());

                                VirtualGigaPowerComponent.flowRuleService.applyFlowRules(serverVlanFlow.build());

                                TrafficSelector.Builder serverSelector = DefaultTrafficSelector.builder();
                                serverSelector.matchInPort(server.getPort());
                                serverSelector.matchVlanId(VlanId.vlanId((short)olt.getSTag()));

                                TrafficTreatment.Builder serverTreatment = DefaultTrafficTreatment.builder();
                                serverTreatment.group(olt.getGroupId(leaf));

                                FlowRule.Builder serverAclRule = DefaultFlowRule.builder();
                                serverAclRule.withSelector(serverSelector.build());
                                serverAclRule.withTreatment(serverTreatment.build());
                                serverAclRule.withPriority(42002);
                                serverAclRule.makePermanent();
                                serverAclRule.fromApp(VirtualGigaPowerComponent.appId);
                                serverAclRule.forTable(ACL_TABLE);
                                serverAclRule.forDevice(leaf.getDeviceId());

                                VirtualGigaPowerComponent.flowRuleService.applyFlowRules(serverAclRule.build());

                                olt.handled();
                                server.handled();

                            }

                            //TODO: preemptive failover from one leaf to the other
                            // - Pseudo wire initiation to spines


                        } else {
                            // OLT and matching server on different pairs

                            //TODO:
                            // - Psuedo wire initiation to spines, with MPLS label to the proper pair






                        }
                        // TODO : - input spines connection match and output to the proper behavior's port

                    }
                }
            }
        }




    }
}
