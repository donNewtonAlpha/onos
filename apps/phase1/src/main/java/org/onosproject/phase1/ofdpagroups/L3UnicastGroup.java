package org.onosproject.phase1.ofdpagroups;

import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.driver.extensions.OfdpaSetVlanVid;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.*;
import org.onosproject.phase1.Phase1Component;

import java.nio.ByteBuffer;
import java.util.Collections;

/**
 * Created by nick on 3/8/16.
 */
public class L3UnicastGroup {

    private static ApplicationId appId;
    private static GroupService groupService;

    public static void initiate(ApplicationId appId, GroupService groupService){
        L3UnicastGroup.appId = appId;
        L3UnicastGroup.groupService = groupService;

    }


    private static Integer intKey(Ip4Prefix ipDst){
        int ipDstKey = ipDst.hashCode() & 0x0fffffff;

        Integer integerGroupId =  (2 << 28 | ipDstKey);

        return integerGroupId;
    }

    public static GroupKey key(Ip4Prefix ipDst){
        Integer integerGroupId =  intKey(ipDst);
        return new DefaultGroupKey(ByteBuffer.allocate(4).putInt(integerGroupId).array());
    }

    public static GroupId create(int portNumber, int vlanId, MacAddress srcMac, MacAddress dstMac, Ip4Prefix ipDst, boolean popVlan, DeviceId device){

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.group(GroupFinder.getL2Interface(portNumber, vlanId, popVlan, device));
        treatment.setEthSrc(srcMac);
        treatment.setEthDst(dstMac);
        treatment.setVlanId(VlanId.vlanId((short)vlanId));

        Integer integerGroupId =  intKey(ipDst);
        final GroupKey groupkey = new DefaultGroupKey(ByteBuffer.allocate(4).putInt(integerGroupId).array());

        GroupFinder.log.info("GroupKey : " + groupkey.toString());

        GroupBucket bucket =
                DefaultGroupBucket.createIndirectGroupBucket(treatment.build());
        GroupDescription groupDescription = new DefaultGroupDescription(device,
                GroupDescription.Type.INDIRECT,
                new GroupBuckets(Collections.singletonList(bucket)),
                groupkey,
                integerGroupId,
                appId);
        groupService.addGroup(groupDescription);

        return groupService.getGroup(device, groupkey).id();

    }
}
