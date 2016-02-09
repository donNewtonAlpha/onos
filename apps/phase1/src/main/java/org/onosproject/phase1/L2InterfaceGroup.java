package org.onosproject.phase1;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.*;

import java.nio.ByteBuffer;
import java.util.Collections;

/**
 * Created by nick on 2/9/16.
 */
public class L2InterfaceGroup {

    private static ApplicationId appId;
    private static GroupService groupService;

    public static void initiate(ApplicationId appId, GroupService groupService){
        L2InterfaceGroup.appId = appId;
        L2InterfaceGroup.groupService = groupService;

    }



    public static GroupKey key(int portNumber, int vlanId){

        Integer integerGroupId =  ((vlanId <<  16) | portNumber);
        return new DefaultGroupKey(ByteBuffer.allocate(4).putInt(integerGroupId).array());
    }

    public static GroupId create(int portNumber, int vlanId, boolean popVlan, DeviceId device){

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.setOutput(PortNumber.portNumber(portNumber));
        if(popVlan){
            treatment.popVlan();
        }



        Integer integerGroupId =  ((vlanId <<  16) | portNumber);
        final GroupKey groupkey = new DefaultGroupKey(ByteBuffer.allocate(4).putInt(integerGroupId).array());

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
