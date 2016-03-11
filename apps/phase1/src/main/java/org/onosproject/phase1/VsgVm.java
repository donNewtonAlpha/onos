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

    private List<Vsg> vsgs;
    private Ip4Address transitNetworkIp;
    private List<VlanId> vlanHandled;
    private MacAddress mac;

    public VsgVm(Ip4Address transitNetworkIp, MacAddress vmMac, List<Integer> vlans){

        this.mac = vmMac;
        this.transitNetworkIp = transitNetworkIp;
        vlanHandled = new LinkedList<>();
        for(Integer i : vlans){
            vlanHandled.add(VlanId.vlanId(i.shortValue()));
        }
        this.vsgs = new LinkedList<>();

    }

    public void addVsg(Vsg newVsg){
        vsgs.add(newVsg);
    }

    public List<Vsg> addVsgs(int n, Ip4Address firstIp, MacAddress firstMac){

        List<Vsg> vsgsAdded = new LinkedList<>();

        for(int i = 0; i < n; i++){
            Vsg newVsg = new Vsg(MacAddress.valueOf(firstMac.toLong() + i), Ip4Address.valueOf(firstIp.toInt()+i));
            vsgs.add(newVsg);
            vsgsAdded.add(newVsg);
        }

        return vsgsAdded;
    }

    public void removeVsg(Ip4Address ip){
        for(Vsg vsg : vsgs){
            if (vsg.getPublicIp().equals(ip)) {
                vsgs.remove(vsg);
            }
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

    public List<Vsg> getVsgs() {
        return vsgs;
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
