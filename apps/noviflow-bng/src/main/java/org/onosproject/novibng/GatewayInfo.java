package org.onosproject.novibng;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by nick on 1/19/17.
 */
public class GatewayInfo {

    private Ip4Address gatewayIp;
    private List<Ip4Prefix> ipBlocks;

    public GatewayInfo(Ip4Address gatewayIp) {
        this.gatewayIp = gatewayIp;
        ipBlocks = new LinkedList<>();
    }


    public Ip4Address getGatewayIp() {
        return gatewayIp;
    }

    /*public List<Ip4Prefix> getIpBlocks() {
        return ipBlocks;
    }*/

    public boolean containsIpBlock(Ip4Prefix prefix) {

        for(Ip4Prefix ipBlock : ipBlocks) {
            if(prefix.equals(ipBlock)) {
                return true;
            }
        }

        return false;
    }

    public void addIpBlock(Ip4Prefix newBlock) {

        if(!this.containsIpBlock(newBlock)) {
            ipBlocks.add(newBlock);
        }
    }
}
