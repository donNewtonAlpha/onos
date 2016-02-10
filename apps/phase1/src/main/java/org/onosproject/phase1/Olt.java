package org.onosproject.phase1;

import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.VlanId;
import org.onosproject.net.PortNumber;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by nick on 2/10/16.
 */
public class Olt {

    private List<VlanId> vlanHandled;



    private PortNumber portNumber;


    public Olt(PortNumber port, List<Integer> vlans){

        vlanHandled = new LinkedList<>();
        for(Integer i : vlans){
            vlanHandled.add(VlanId.vlanId(i.shortValue()));
        }
        portNumber = port;

    }

    public Olt(int port, List<Integer> vlans){

        vlanHandled = new LinkedList<>();
        for(Integer i : vlans){
            vlanHandled.add(VlanId.vlanId(i.shortValue()));
        }
        portNumber = PortNumber.portNumber(port);

    }

    public List<VlanId> getVlanHandled() {
        return vlanHandled;
    }

    public void setVlanHandled(List<VlanId> vlanHandled) {
        this.vlanHandled = vlanHandled;
    }

    public PortNumber getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(PortNumber portNumber) {
        this.portNumber = portNumber;
    }

    public String toString(){
        return "port number : " + portNumber + ", contains " + vlanHandled.size() + " vlans !";
    }
}
