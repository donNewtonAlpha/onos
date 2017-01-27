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

import java.nio.ByteBuffer;
import java.util.HashMap;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.driver.extensions.NoviflowSetVxLan;
import org.onosproject.net.DeviceId;
import org.onosproject.core.GroupId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.*;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.DefaultGroupDescription;
import org.slf4j.Logger;

import java.util.List;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;

import static org.slf4j.LoggerFactory.getLogger;


/**
 * Created by nick on 8/11/16.
 */
public class MulticastHandler {

    private final Logger log = getLogger(getClass());

    private static final int DELAY = 200;

    private FlowRuleService flowRuleService;
    private GroupService groupService;

    private ApplicationId appId;

    private DeviceId deviceId;
    private HashMap<Integer, GroupId> multicastGroups;
    private HashMap<Ip4Address, FlowRule> multicastFlows;
    private Lock lock;

    private MulticastGroupCleaningThread thread;



    public MulticastHandler(DeviceId deviceId, FlowRuleService flowRuleService, GroupService groupService, ApplicationId appId) {

        this.flowRuleService = flowRuleService;
        this.groupService = groupService;
        this.deviceId = deviceId;
        this.appId = appId;

        multicastFlows = new HashMap<>();
        multicastGroups = new HashMap<>();

        lock = new ReentrantLock();


    }

    public void addAccessNodeToFeed(Ip4Address feedIp, PortNumber port) {

        //Check if the flow already exists
        FlowRule oldFlow = multicastFlows.get(feedIp);
        int mapping = 0;
        if (oldFlow != null) {
            //flow already exists, extract its current mapping
            mapping = extractMappingFromFlow(oldFlow);
        }
        if (mappingContainsPort(mapping, port)) {
            log.warn("This port already receives this feed");
            return;
        }
        mapping = addPortToMapping(mapping, port);

        //Check if the matching group already exists
        GroupId matchingGroup = multicastGroups.get(mapping);

        if (matchingGroup == null){
            //Need to create it
            matchingGroup = createGroup(mapping);
            multicastGroups.put(mapping, matchingGroup);
        }

        FlowRule newFlow = createFlow(feedIp, matchingGroup, 5000 + getWatchingPortsFromMapping(mapping));

        flowRuleService.applyFlowRules(newFlow);

        if (oldFlow != null) {

            multicastFlows.replace(feedIp, newFlow);
            delayedRemoveFlow(oldFlow);
        } else {
            multicastFlows.put(feedIp, newFlow);
        }




    }

    public void removeAccessNodeToFeed(Ip4Address feedIp, PortNumber port) {

        //Check if the flow already exists
        FlowRule oldFlow = multicastFlows.get(feedIp);
        int oldMapping = 0;
        if (oldFlow != null) {
            //flow already exists, extract its current mapping
            oldMapping = extractMappingFromFlow(oldFlow);
        } else {
            log.error("Tried to remove access node from an empty feed");
            return;
        }

        if (oldMapping == 0) {
            log.error("Port mapping is 0 !!");
            return;
        }

        if (!mappingContainsPort(oldMapping, port)) {
            log.warn("This port does not receive this feed, impossible to remove it from the feed");
            return;
        }

        int mapping = removePortFromMapping(oldMapping, port);

        if (mapping != 0) {

            //Check if the matching group already exists
            GroupId matchingGroup = multicastGroups.get(mapping);

            if (matchingGroup == null) {
                //Need to create it
                matchingGroup = createGroup(mapping);
                multicastGroups.put(mapping, matchingGroup);
            }

            FlowRule newFlow = createFlow(feedIp, matchingGroup, 5000 + getWatchingPortsFromMapping(mapping));

            flowRuleService.applyFlowRules(newFlow);

            if (oldFlow != null) {

                multicastFlows.replace(feedIp, newFlow);
                delayedRemoveFlow(oldFlow);
            } else {
                multicastFlows.put(feedIp, newFlow);
            }
        } else {
            //This flow is no longer used
            flowRuleService.removeFlowRules(oldFlow);
        }


    }

    private GroupId extractGrpoupIdFromFlow(FlowRule flow) {

        List<Instruction> instructions = flow.treatment().immediate();
        for (Instruction instruction : instructions) {
            if (instruction.type() == Instruction.Type.GROUP) {

                Instructions.GroupInstruction groupInstruction = (Instructions.GroupInstruction) instruction;
                return groupInstruction.groupId();

            }
        }

        return null;


    }

    private int extractMappingFromFlow(FlowRule flow) {


        GroupId flowGroupId = extractGrpoupIdFromFlow(flow);

        Iterable<Group> groups = groupService.getGroups(deviceId, appId);
        for (Group group : groups) {

            if (group.id().equals(flowGroupId)) {
                //Group found, extract buckets
                return extractMappingFromGroup(group);
            }
        }


        return 0;
    }

    private int extractMappingFromGroup(Group group) {

        int mapping = 0;

        List<GroupBucket> buckets = group.buckets().buckets();
        for (GroupBucket bucket : buckets) {

            List<Instruction> bucketInstructions = bucket.treatment().immediate();
            for (Instruction bucketInstruction : bucketInstructions) {
                if (bucketInstruction.type() == Instruction.Type.OUTPUT) {
                    mapping = addPortToMapping(mapping, ((Instructions.OutputInstruction)bucketInstruction).port());
                }
            }
        }

        return mapping;

    }

    private FlowRule createFlow(Ip4Address feedIp, GroupId groupId, int priority) {

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchIPDst(feedIp.toIpPrefix());

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.setVlanId(VlanId.vlanId((short)4000));
        treatment.group(groupId);

        FlowRule.Builder rule = DefaultFlowRule.builder();
        rule.withSelector(selector.build());
        rule.withTreatment(treatment.build());
        rule.withPriority(priority);
        rule.forTable(0);
        rule.fromApp(appId);
        rule.forDevice(deviceId);
        rule.makePermanent();

        return rule.build();
    }

    private GroupId createGroup(int portMapping) {
        return createGroup(portMapping, portMapping);
    }

    public GroupId createGroup(int portMapping, int groupKey) {

        List<PortNumber> ports = getPortsFromMapping(portMapping);

        GroupKey key = new DefaultGroupKey(ByteBuffer.allocate(4).putInt(groupKey).array());;

        List<GroupBucket> buckets = new LinkedList<>();


        for (PortNumber port : ports) {

            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            treatment.setOutput(port);

            buckets.add(DefaultGroupBucket.createAllGroupBucket(treatment.build()));

        }

        GroupDescription groupDescription = new DefaultGroupDescription(deviceId,
                GroupDescription.Type.ALL,
                new GroupBuckets(buckets),
                key,
                portMapping,
                appId);

        groupService.addGroup(groupDescription);

        return groupService.getGroup(deviceId, key).id();

    }

    private List<GroupId> groupsInUse() {

        List<GroupId> groupsInUse = new LinkedList<>();

        for (FlowRule flow : multicastFlows.values()) {

            GroupId groupId = extractGrpoupIdFromFlow(flow);
            if (!groupsInUse.contains(groupId)) {
                groupsInUse.add(groupId);
            }

        }

        return groupsInUse;
    }

    public List<GroupId> outdatedGroups() {

        List<GroupId> registeredGroups = new LinkedList<>(multicastGroups.values());
        List<GroupId> groupsInUse = groupsInUse();

        //Remove the groups actually in use from the one registered
        registeredGroups.removeAll(groupsInUse);

        //the remainder is outdated
        return registeredGroups;

    }

    private List<PortNumber> getPortsFromMapping(int portMapping){

        List<PortNumber> ports = new LinkedList<>();

        for (int i = 0; i < 31; i++) {

             if ((portMapping >> i)%2 == 1) {
                //this port is included
                ports.add(PortNumber.portNumber(i));
            }

        }

        return ports;
    }

    private int getMappingFromPorts(List<PortNumber> ports) {

        int mapping = 0;
        for (PortNumber port :ports) {
            mapping = mapping + 2^((int)port.toLong());
        }

        return mapping;

    }

    private int getWatchingPortsFromMapping(int portMapping) {

        int watchers = 0;

        for (int i = 0; i < 31; i++) {
            if ((portMapping >> i)%2 == 1) {
                //this port is included
                watchers ++;
            }
        }

        return watchers;

    }

    private int addPortToMapping(int portMapping, PortNumber newPort) {
        return portMapping + 2^((int)newPort.toLong());
    }

    private int removePortFromMapping(int portMapping, PortNumber portToRemove) {
        return portMapping - 2 ^ ((int)portToRemove.toLong());
    }

    private boolean mappingContainsPort(int mapping, PortNumber port) {

        if ((mapping >> (int)port.toLong()) % 2 == 1) {
            return true;
        }
        return false;

    }

    private void delayedRemoveFlow(FlowRule flow) {
        delayedAction(flow, false);
    }

    private void delayedAddFlow(FlowRule flow) {
        delayedAction(flow, true);
    }

    private void delayedAction(FlowRule flow, boolean add) {

        Runnable r = new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(DELAY);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (add) {
                    flowRuleService.applyFlowRules(flow);
                } else {
                    flowRuleService.removeFlowRules(flow);
                }
            }
        };

        Thread t = new Thread(r);
        t.setDaemon(true);
        t.start();

    }

    public void setThread(MulticastGroupCleaningThread t) {
        thread = t;
    }

    public DeviceId getDeviceId() {
        return deviceId;
    }

    public ApplicationId getAppId() {
        return appId;
    }

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }

    public void kill() {
        thread.kill();
        unlock();
    }
}
