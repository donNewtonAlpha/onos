package org.onosproject.noviaggswitch;

import org.omg.CORBA.Object;
import org.onlab.packet.Ip4Address;
import org.onosproject.net.DeviceId;

/**
 * Created by nick on 9/20/16.
 */
public class VxlanTunnelId {

    private DeviceId deviceId;
    private Ip4Address ip;
    private int vni;

    public VxlanTunnelId(DeviceId deviceId, Ip4Address ip, int vni) {
        this.deviceId = deviceId;
        this.ip = ip;
        this.vni = vni;
    }

    public boolean equals(Object object) {

        if(object instanceof VxlanTunnelId) {
            VxlanTunnelId other = (VxlanTunnelId) object;
            if(other.deviceId.equals(deviceId) && other.ip.equals(ip) && other.vni == vni) {
                return true;
            }
        }
        return false;
    }

}
