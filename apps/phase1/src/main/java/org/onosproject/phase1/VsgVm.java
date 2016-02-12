package org.onosproject.phase1;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by nick on 2/11/16.
 */
public class VsgVm {

    private Ip4Prefix ipBlock;
    private Ip4Address transitNetworkIp;
    private List<VlanId> vlanHandled;
    private MacAddress mac;

    public VsgVm(Ip4Address transitNetworkIp, MacAddress vmMac, Ip4Prefix ipBlock, List<Integer> vlans){

        this.ipBlock = ipBlock;
        this.mac = vmMac;
        this.transitNetworkIp = transitNetworkIp;
        vlanHandled = new LinkedList<>();
        for(Integer i : vlans){
            vlanHandled.add(VlanId.vlanId(i.shortValue()));
        }

    }

    public void addVlan(int newVlan){
        addVlan(VlanId.vlanId((short)newVlan));
    }

    public void addVlan(VlanId newVlan){
        //Check if it is new
        for(VlanId vlan : vlanHandled){
            if(vlan.equals(newVlan)){
                return;
            }
        }
        vlanHandled.add(newVlan);
    }

    public void removeVlan(int vlanToRemove){
        removeVlan(VlanId.vlanId((short) vlanToRemove));
    }

    public void removeVlan(VlanId vlanToRemove){
        vlanHandled.remove(vlanToRemove);
    }

    public Ip4Prefix getIpBlock() {
        return ipBlock;
    }

    public Ip4Address getTransitNetworkIp() {
        return transitNetworkIp;
    }

    public List<VlanId> getVlanHandled() {
        return vlanHandled;
    }

    public MacAddress getMac() {
        return mac;
    }
}
