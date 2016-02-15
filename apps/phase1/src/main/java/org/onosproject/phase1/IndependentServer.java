package org.onosproject.phase1;

import org.onlab.packet.MacAddress;
import org.onosproject.net.PortNumber;

/**
 * Created by nick on 2/15/16.
 */
public class IndependentServer extends NetworkElement {

    MacAddress mac;

    public IndependentServer(int port, MacAddress mac){
        this.setPortNumber(PortNumber.portNumber(port));
        this.mac = mac;
    }

    public MacAddress getMac() {
        return mac;
    }
}
