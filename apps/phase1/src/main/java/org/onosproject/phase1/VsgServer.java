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
    private Ip4Prefix ipPrefix;
    private PortNumber portNumber;


    public VsgServer(PortNumber port, List<Integer> vlans, Ip4Prefix ip4Prefix){

        vlanHandled = new LinkedList<>();
        ipPrefix = ip4Prefix;
        portNumber = port;
        for(Integer i : vlans){
            vlanHandled.add(VlanId.vlanId(i.shortValue()));
        }

    }

    public VsgServer(PortNumber port, List<Integer> vlans, String ip4Prefix){
        new VsgServer(port, vlans, Ip4Prefix.valueOf(ip4Prefix));
    }

    public VsgServer(PortNumber port, List<Integer> vlans, String ipPrefix, int prefixLength){
        new VsgServer(port, vlans, Ip4Prefix.valueOf(Ip4Address.valueOf(ipPrefix),prefixLength));
    }

    public VsgServer(PortNumber port, List<Integer> vlans){
        vlanHandled = new LinkedList<>();
        portNumber = port;
        for(Integer i : vlans){
            vlanHandled.add(VlanId.vlanId(i.shortValue()));
        }
    }

    public VsgServer(int port, List<Integer> vlans){
        new VsgServer(PortNumber.portNumber(port), vlans);
    }




    public List<VlanId> getVlanHandled() {
        return vlanHandled;
    }

    public void setVlanHandled(List<VlanId> vlanHandled) {
        this.vlanHandled = vlanHandled;
    }



    public Ip4Prefix getIpPrefix() {
        return ipPrefix;
    }

    public void setIpPrefix(Ip4Prefix ipPrefix) {
        this.ipPrefix = ipPrefix;
    }


    public PortNumber getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(PortNumber portNumber) {
        this.portNumber = portNumber;
    }
}
