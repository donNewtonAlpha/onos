package org.onosproject.phase1;

import org.onlab.packet.MacAddress;
import org.onosproject.net.PortNumber;

/**
 * Created by nick on 2/11/16.
 */
public class QuaggaInstance extends NetworkElement {

    private MacAddress mac;

    public QuaggaInstance(int port, MacAddress mac){

        super.setPortNumber(PortNumber.portNumber((short)port));
        this.mac = mac;
    }


    public MacAddress getMac() {
        return mac;
    }
}
