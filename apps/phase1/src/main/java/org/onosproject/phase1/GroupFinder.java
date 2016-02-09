package org.onosproject.phase1;

import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.group.GroupKey;

/**
 * Created by nick on 2/9/16.
 */
public class GroupFinder {

    private static GroupService groupService;

    public static void initiate(ApplicationId appId, GroupService groupService){
        GroupFinder.groupService = groupService;
        L2InterfaceGroup.initiate(appId, groupService);
    }

    public static GroupId getL2Interface(int portNumber, int vlanId, DeviceId deviceId) {
        return  getL2Interface(portNumber, vlanId, false, deviceId);
    }

    public static GroupId getL2Interface(PortNumber portNumber, VlanId vlanId, DeviceId deviceId) {
        return  getL2Interface((int)(portNumber.toLong()), vlanId.toShort(), false, deviceId);
    }

    public static GroupId getL2Interface(PortNumber portNumber, VlanId vlanId, boolean popVlan, DeviceId deviceId) {
        return  getL2Interface((int)(portNumber.toLong()), vlanId.toShort(), popVlan, deviceId);
    }

    public static GroupId getL2Interface(int portNumber, int vlanId, boolean popVlan, DeviceId deviceId){

        GroupKey key = L2InterfaceGroup.key(portNumber, vlanId);

        Group matchingGroup = groupService.getGroup(deviceId, key);

        if(matchingGroup != null){
            return matchingGroup.id();
        }else{
            //Create the group and add it to the list
            GroupId newL2InterfaceGroupId = L2InterfaceGroup.create(portNumber, vlanId, popVlan, deviceId);

            return newL2InterfaceGroupId;

        }

    }



}
