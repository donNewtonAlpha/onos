package org.onosproject.virtualgigapower;

import org.onlab.packet.VlanId;
import org.onosproject.core.GroupId;
import org.onosproject.net.PortNumber;

/**
 * Created by nick on 10/12/15.
 */
public abstract class SwitchingBehavior {

    private boolean handled = false;
    protected PortNumber port;
    private GroupId groupId;

    public boolean isHandled(){
        return handled;
    }

    public void handled(){
        handled = true;
    }

    public static VlanId oltVlanToServerVlan(VlanId oltVlan){
        return VlanId.vlanId((short)(oltVlanToServerVlan(oltVlan.toShort())));
    }

    public static VlanId serverVlanToOltVlan(VlanId serverVlan){
        return VlanId.vlanId((short)(serverVlanToOltVlan(serverVlan.toShort())));
    }
    public static int oltVlanToServerVlan(int oltVlan){
        return oltVlan + 1000;
    }

    public static int serverVlanToOltVlan(int serverVlan){
        return serverVlan - 1000;
    }

    public PortNumber getPort(){
        return port;
    }

    protected abstract void initiateGroup(Leaf device);


    public GroupId getGroupId(Leaf device){
        if(groupId == null){
            this.initiateGroup(device);
        }
        return groupId;
    }
}


