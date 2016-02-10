package org.onosproject.phase1;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.VlanId;
import org.onosproject.net.PortNumber;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by nick on 2/10/16.
 */
public class VsgServer {

    private List<VlanId> vlanHandled;
    //private Ip4Prefix ipPrefix;
    private PortNumber portNumber;


    public VsgServer(int port, List<Integer> vlans){

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



   /* public Ip4Prefix getIpPrefix() {
        return ipPrefix;
    }

    public void setIpPrefix(Ip4Prefix ipPrefix) {
        this.ipPrefix = ipPrefix;
    }*/


    public PortNumber getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(PortNumber portNumber) {
        this.portNumber = portNumber;
    }

    public String toString(){
        return "port number : " + portNumber + ", contains " + vlanHandled.size() + "vlans";
    }
}
