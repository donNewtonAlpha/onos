package org.onosproject.virtualgigapower;

import org.onlab.packet.VlanId;

/**
 * Created by nick on 10/12/15.
 */
public abstract class SwitchingBehavior {

    private boolean handled = false;

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
}


