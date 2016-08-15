package org.onosproject.noviaggswitch;

import java.nio.ByteBuffer;
import java.util.Collection;
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

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by nick on 8/15/16.
 */
public class MulticastGroupCleaningThread extends Thread {

    private final int WAKE_UP_PERIOD = 5000;

    private MulticastHandler multicastHandler;
    private GroupService groupService;

    public MulticastGroupCleaningThread (MulticastHandler multicastHandler, GroupService groupService) {

        this.multicastHandler = multicastHandler;
        this.groupService = groupService;

    }

    public void run() {

        while(true) {

            multicastHandler.lock();

            List<GroupId> outdateGroups = multicastHandler.outdatedGroups();
            Iterable<Group> allGroups = groupService.getGroups(multicastHandler.getDeviceId(), multicastHandler.getAppId());
            for(Group group : allGroups) {
                for (GroupId groupToRemove : outdateGroups) {
                    if(group.id().equals(groupToRemove)){
                        //Group to remove found
                        groupService.removeGroup(group.deviceId(), group.appCookie(), group.appId());
                        outdateGroups.remove(groupToRemove);

                    }
                }
            }

            multicastHandler.unlock();

            try {
                Thread.sleep(WAKE_UP_PERIOD);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


        }


    }





}
