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
        return VlanId.vlanId((short)(oltVlan.toShort() + 100));
    }

    public static VlanId serverVlanToOltVlan(VlanId serverVlan){
        return VlanId.vlanId((short)(serverVlan.toShort() - 100));
    }

    public PortNumber getPort(){
        return port;
    }

    private void initiateGroup(Switch device){
        //TODO
    }

    public GroupId getGroupId(Switch device){
        if(groupId == null){
            this.initiateGroup(device);
        }
        return groupId;
    }
}


