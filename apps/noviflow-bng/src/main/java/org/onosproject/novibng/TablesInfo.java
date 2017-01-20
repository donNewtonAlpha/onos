package org.onosproject.novibng;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by nick on 1/19/17.
 */
public class TablesInfo {

    public static final int MAX_SUBSCRIBERS = 1024;
    public static final int CONSECUTIVES_TABLES = 4;

    public static final byte[] DSCP_LEVELS = {1, 2, 3};


    private int rootTable;
    private List<Ip4Prefix> ipBlocks;
    private int subscribers;

    public TablesInfo(int table){
        rootTable = table;
        subscribers = 0;
        ipBlocks = new LinkedList<>();
    }

    public boolean tryAddIpBlock(Ip4Prefix ipBlock) {
        if(subscribers + Math.pow(2, 32 - ipBlock.prefixLength()) > MAX_SUBSCRIBERS) {
            return false;
        } else {
            ipBlocks.add(ipBlock);
            subscribers += (int) Math.pow(2, 32 - ipBlock.prefixLength());
            return true;
        }
    }

    public boolean containsIp(Ip4Address ip) {

        for(Ip4Prefix prefix : ipBlocks) {
            if(prefix.contains(ip)){
                return true;
            }
        }

        return false;
    }

    public boolean containsBlock(Ip4Prefix block) {

        for (Ip4Prefix prefix : ipBlocks) {
            if(prefix.equals(block)) {
                return true;
            }
        }
        return false;
    }

    public int getRootTable(){
        return rootTable;
    }


}
