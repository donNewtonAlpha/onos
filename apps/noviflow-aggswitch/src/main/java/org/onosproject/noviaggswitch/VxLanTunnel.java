package org.onosproject.noviaggswitch;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.PortNumber;

import java.util.LinkedList;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Created by nick on 8/16/16.
 */
public class VxLanTunnel {


    private PortNumber port;
    private Ip4Address dstIp;
    private int vni;
    private MacAddress primaryViaMac = null;
    private MacAddress secondaryViaMac = null;
    private int weight;

    private List<FlowRule> flows = new LinkedList<>();

    public VxLanTunnel(Ip4Address ip) {
        this.dstIp = ip;
    }

    public void setVni(int vni) {
        this.vni = vni;
    }

    public void setPort(PortNumber port) {
        this.port = port;
    }

    public void addFlow(FlowRule flow) {
        flows.add(flow);
    }

    public List<FlowRule> getFlows() {
        return flows;
    }

    public void setViaMac(int priority, MacAddress viaMac){
        if(primaryViaMac != null) {
            if (priority > weight) {
                //this is primary
                secondaryViaMac = primaryViaMac;
                primaryViaMac = viaMac;
            } else {
                //This is secondary
                secondaryViaMac = viaMac;
            }
        } else {
            weight = priority;
            primaryViaMac = viaMac;
        }

    }

    public boolean match(Ip4Address ip) {
        return this.dstIp.equals(ip);
    }

    public boolean match(Ip4Address ip, int vni) {
        return this.dstIp.equals(ip);
    }


    public PortNumber getPort() {
        return port;
    }

    public Ip4Address getDstIp() {
        return dstIp;
    }

    public int getVni() {
        return vni;
    }

    public MacAddress getPrimaryViaMac() {
        return primaryViaMac;
    }

    public MacAddress getSecondaryViaMac() {
        return secondaryViaMac;
    }

    public boolean isValid() {
        return port != null && dstIp != null && vni != 0 && primaryViaMac != null && secondaryViaMac != null;
    }

    public ObjectNode jsonNode(){

        ObjectNode json = new ObjectMapper().createObjectNode();
        json.put("port", port.toLong());
        json.put("vxlanIp", dstIp.toString());
        json.put("vni", vni);
        if(primaryViaMac != null) {
            json.put("primaryViaMac", primaryViaMac.toString());
        }
        if(secondaryViaMac != null) {
            json.put("secondaryViaMac", secondaryViaMac.toString());
        }

        return json;


    }

}
