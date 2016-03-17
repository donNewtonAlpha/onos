package org.onosproject.phase1.config;

import org.onlab.packet.VlanId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by nick on 3/17/16.
 */
public class VlanCrossconnect {

    private VlanId vlan;
    private List<PortNumber> ports;

    public VlanCrossconnect(VlanId vlanId, int port1, int port2) {
        vlan = vlanId;
        ports = new LinkedList<>();
        ports.add(PortNumber.portNumber(port1));
        ports.add(PortNumber.portNumber(port2));
    }

    public VlanId getVlanId(){
        return vlan;
    }

    public List<PortNumber> getPorts(){
        return ports;
    }

    public String toString(){
        StringBuilder builder = new StringBuilder();
        builder.append("Vlan : ");
        builder.append(getVlanId());
        builder.append(", ");
        builder.append("Ports : ");
        for(PortNumber port : getPorts()) {
            builder.append(port);
            builder.append(", ");
        }

        return builder.toString();
    }

}
