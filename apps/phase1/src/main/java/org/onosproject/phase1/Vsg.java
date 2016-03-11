package org.onosproject.phase1;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;

/**
 * Created by nick on 3/8/16.
 */
public class Vsg {

    private MacAddress wanSideMac;
    private Ip4Address publicIp;

    public Vsg(MacAddress mac, Ip4Address ip){
        this.wanSideMac = mac;
        this.publicIp = ip;
    }

    public MacAddress getWanSideMac() {
        return wanSideMac;
    }

    public Ip4Address getPublicIp() {
        return publicIp;
    }

    public void hello(){
        Phase1Component.log.debug("Hello from Vsg");
    }
}
