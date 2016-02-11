package org.onosproject.phase1;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.PortNumber;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by nick on 2/10/16.
 */
public class VsgServer extends NetworkElement{

    private List<VsgVm> vms;
    private Ip4Prefix ipBlock;
    private MacAddress mac;


    public VsgServer(int port, MacAddress serverMac, List<VsgVm> vms, Ip4Prefix prefix){

        this.mac = serverMac;
        super.setPortNumber(PortNumber.portNumber(port));
        this.vms = vms;
        this.ipBlock = prefix;

    }



    public List<VlanId> getVlanHandled() {

        LinkedList<VlanId> vlans = new LinkedList<>();

        for(VsgVm vm : vms){
            vlans.addAll(vm.getVlanHandled());
        }

        return vlans;
    }

    public void addVm(VsgVm newVm){
        //check if it is already there
        for(VsgVm vm : vms){
            if(vm.getTransitNetworkIp().equals(newVm.getTransitNetworkIp())){
                return;
            }
        }
        vms.add(newVm);
    }

    public List<VsgVm> getVms() {
        return vms;
    }

    public Ip4Prefix getIpBlock() {
        return ipBlock;
    }

    public void setIpBlock(Ip4Prefix ipBlock) {
        this.ipBlock = ipBlock;
    }



    public MacAddress getMac() {
        return mac;
    }

    public String toString(){
        return "port number : " + getPortNumber() + ", contains " + vms.size() + " vms";
    }
}
