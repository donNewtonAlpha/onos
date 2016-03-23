package org.onosproject.phase1.ofdpagroups;

import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.*;
import org.onosproject.phase1.config.VlanCrossconnect;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by nick on 3/21/16.
 */
public class L2MulticastGroup {

    private static ApplicationId appId;
    private static GroupService groupService;

    public static void initiate(ApplicationId appId, GroupService groupService){
        L2MulticastGroup.appId = appId;
        L2MulticastGroup.groupService = groupService;

    }

    private static Integer intKey(int vlanId){
        Integer integerGroupId =  ((3 << 28) | (vlanId << 16));
        return integerGroupId;
    }

    public static GroupKey key(VlanId vlanId){

        int integerGroupId = intKey(vlanId.toShort());
        return new DefaultGroupKey(ByteBuffer.allocate(4).putInt(integerGroupId).array());
    }

    public static GroupId create(List<PortNumber> ports, VlanId  vlanId, DeviceId device){


        final GroupKey groupkey = key(vlanId);
        List<GroupBucket> buckets = new LinkedList<>();

        for(PortNumber port : ports) {
            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            treatment.group(GroupFinder.getL2Interface(port, vlanId, false, device));

            GroupBucket bucket = DefaultGroupBucket.createIndirectGroupBucket(treatment.build());

            buckets.add(bucket);
        }

        GroupDescription groupDescription = new DefaultGroupDescription(device,
                GroupDescription.Type.INDIRECT,
                new GroupBuckets(buckets),
                groupkey,
                intKey(vlanId.toShort()),
                appId);
        groupService.addGroup(groupDescription);

        return groupService.getGroup(device, groupkey).id();

    }

}
