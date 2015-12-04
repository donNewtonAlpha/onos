package org.onosproject.virtualgigapower;

import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

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
        //TODO : output group  for  this behavior
    }

}
