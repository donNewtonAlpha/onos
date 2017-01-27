/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        if (subscribers + Math.pow(2, 32 - ipBlock.prefixLength()) > MAX_SUBSCRIBERS) {
            return false;
        } else {
            ipBlocks.add(ipBlock);
            subscribers += (int) Math.pow(2, 32 - ipBlock.prefixLength());
            return true;
        }
    }

    public boolean containsIp(Ip4Address ip) {

        for (Ip4Prefix prefix : ipBlocks) {
            if (prefix.contains(ip)) {
                return true;
            }
        }

        return false;
    }

    public boolean containsBlock(Ip4Prefix block) {

        for (Ip4Prefix prefix : ipBlocks) {
            if (prefix.equals(block)) {
                return true;
            }
        }
        return false;
    }

    public int getRootTable(){
        return rootTable;
    }


}
