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

package org.onosproject.secondbng;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.FlowRule;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by nick on 1/19/17.
 */
public class SubscriberInfo {

    private PortNumber port;
    private VlanId cTag;
    private VlanId sTag;
    private int uploadSpeed;
    private int downloadSpeed;
    private MacAddress mac;
    private Ip4Address gatewayIp;
    private List<FlowRule> flows;
    private boolean standby;
    private TablesInfo tableInfo;

    public SubscriberInfo() {
        this.port = null;
        this.cTag = null;
        this.sTag = null;
        this.mac = null;
        this.gatewayIp = null;
        this.flows = new LinkedList<>();
        tableInfo = null;
        standby = true;
    }


    public PortNumber getPort() {
        return port;
    }

    public VlanId getCTag() {
        return cTag;
    }

    public VlanId getSTag() {
        return sTag;
    }

    public int getUploadSpeed() {
        return uploadSpeed;
    }

    public int getDownloadSpeed() {
        return downloadSpeed;
    }

    public List<FlowRule> getFlows() {
        return flows;
    }

    public void addFlow(FlowRule flow) {
        flows.add(flow);
    }

    public MacAddress getMac() {
        return mac;
    }

    public Ip4Address getGatewayIp() {
        return gatewayIp;
    }

    public TablesInfo getTableInfo() {
        return tableInfo;
    }

    public void setTableInfo(TablesInfo tableInfo) {
        this.tableInfo = tableInfo;
    }

    public void setGatewayIp(Ip4Address gatewayIp) {
        this.gatewayIp = gatewayIp;
    }

    public void setMac(MacAddress mac) {
        this.mac = mac;
    }

    public void setUploadSpeed(int uploadSpeed) {
        this.uploadSpeed = uploadSpeed;
    }

    public void setDownloadSpeed(int downloadSpeed) {
        this.downloadSpeed = downloadSpeed;
    }

    public void setPort(PortNumber port) {
        this.port = port;
        this.standby = false;
    }

    public void setCTag(VlanId cTag) {
        this.cTag = cTag;
    }

    public void setSTag(VlanId sTag) {
        this.sTag = sTag;
    }

    public boolean isStandby() {
        return standby;
    }
}
