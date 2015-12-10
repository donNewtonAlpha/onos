package org.onosproject.virtualgigapower;

import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.*;

import java.nio.ByteBuffer;
import java.util.Collections;

/**
 * Created by nick on 10/12/15.
 */
public class CustomerServerBehavior extends SwitchingBehavior {


    public final static VlanId INTERNET_VLAN = VlanId.vlanId((short)404);

    private PortNumber serverConnectionPort;
    private int sTag;

    public CustomerServerBehavior(int portNumber, int sTag){
        this.sTag = sTag;
        this.port = PortNumber.portNumber(portNumber);
    }

    public int getSTag(){
        return sTag;
    }



    protected void initiateGroup(Leaf leaf){

        TrafficTreatment.Builder output = DefaultTrafficTreatment.builder();
        output.setOutput(this.port);


        Integer behaviorGroupId =  ((sTag <<  16) |((short) this.port.toLong()));
        final GroupKey behaviorGroupkey = new DefaultGroupKey(ByteBuffer.allocate(4).putInt(behaviorGroupId).array());

        GroupBucket behaviorBucket =
                DefaultGroupBucket.createIndirectGroupBucket(output.build());
        GroupDescription behaviorGroupDescription = new DefaultGroupDescription(leaf.getDeviceId(),
                GroupDescription.Type.INDIRECT,
                new GroupBuckets(Collections.singletonList(behaviorBucket)),
                behaviorGroupkey,
                behaviorGroupId,
                VirtualGigaPowerComponent.appId);

        VirtualGigaPowerComponent.groupService.addGroup(behaviorGroupDescription);

        this.groupIds.put(leaf.getDeviceId(),VirtualGigaPowerComponent.groupService.getGroup(leaf.getDeviceId(), behaviorGroupkey).id());
    }

}
