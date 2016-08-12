package org.onosproject.noviaggswitch;

import java.nio.ByteBuffer;
import java.util.HashMap;

import org.onlab.packet.Ip4Address;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.core.GroupId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.DefaultGroupDescription;
import java.util.List;
import java.util.LinkedList;


/**
 * Created by nick on 8/11/16.
 */
public class MulticastHandler {

    private FlowRuleService flowRuleService;
    private GroupService groupService;

    private ApplicationId appId;

    private DeviceId deviceId;
    private HashMap<Integer, GroupId> multicastGroups;
    private HashMap<Ip4Address, FlowRule> multicastFlows;



    public MulticastHandler(DeviceId deviceId, FlowRuleService flowRuleService, GroupService groupService, ApplicationId appId) {

        this.flowRuleService = flowRuleService;
        this.groupService = groupService;
        this.deviceId = deviceId;
        this.appId = appId;

        multicastFlows = new HashMap<>();
        multicastGroups = new HashMap<>();

    }

    public void addAccessNodeToFeed(Ip4Address feedIp, PortNumber port) {


    }

    public void removeAccessNodeToFeed(Ip4Address feedIp, PortNumber port) {


        //TODO: check if group still in use, if no remove it
        
    }

    private GroupId creategroup(int portMapping) {
        return createGroup(portMapping, portMapping);
    }

    public GroupId createGroup(int portMapping, int groupKey) {

        List<PortNumber> ports = getPortsFromMapping(portMapping);

        GroupKey key = new DefaultGroupKey(ByteBuffer.allocate(4).putInt(groupKey).array());;

        List<GroupBucket> buckets = new LinkedList<>();


        for(PortNumber port : ports) {

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

    private List<PortNumber> getPortsFromMapping(int portMapping){

        List<PortNumber> ports = new LinkedList<>();

        for(int i = 0; i < 31; i++) {

            if((portMapping >> i)%2 == 1) {
                //this port is included
                ports.add(PortNumber.portNumber(i));
            }

        }

        return ports;
    }

    private int getMappingFromPorts(List<PortNumber> ports) {

        int mapping = 0;
        for(PortNumber port :ports) {
            mapping = mapping + 2^((int)port.toLong());
        }

        return mapping;

    }

    private int addPortToMapping(int portMapping, PortNumber newPort) {
        return portMapping + 2^((int)newPort.toLong());
    }

    private int removePortFromMapping(int portMapping, PortNumber portToRemove) {
        return portMapping - 2 ^ ((int)portToRemove.toLong());
    }




    public DeviceId getDeviceId() {
        return deviceId;
    }
}
