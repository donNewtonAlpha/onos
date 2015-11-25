package org.onosproject.virtualgigapower;

import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

/**
 * Created by nick on 10/12/15.
 */
public class OltBehavior extends SwitchingBehavior {

    public final static VlanId OLT_MULTICAST_VLAN = VlanId.vlanId((short)2000);

    private ConnectPoint oltConnection;
    private int sTag;

    public OltBehavior(DeviceId deviceId, int portNumber, int sTag){
        this.sTag = sTag;
        oltConnection = new ConnectPoint(deviceId, PortNumber.portNumber(portNumber));
    }





}
