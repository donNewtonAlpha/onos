package org.onosproject.noviaggswitch;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.MacAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by nick on 8/22/16.
 */
public class AggDeviceConfig {

    private Logger log = LoggerFactory.getLogger(getClass());

    private DeviceId deviceId;
    private Ip4Address loopbackIP;
    private Ip4Prefix primaryLinkSubnet;
    private Ip4Prefix secondaryLinkSubnet;
    private MacAddress primaryLinkMac;
    private MacAddress secondaryLinkMac;
    private PortNumber primaryLinkPort;
    private PortNumber secondaryLinkPort;

    public AggDeviceConfig(DeviceId deviceId, Ip4Address loopbackIP, Ip4Prefix primaryLinkSubnet, Ip4Prefix secondaryLinkSubnet,
                           MacAddress primaryLinkMac, MacAddress secondaryLinkMac, PortNumber primaryLinkPort, PortNumber secondaryLinkPort) {

        this.deviceId = deviceId;
        this.loopbackIP = loopbackIP;
        this.primaryLinkSubnet = primaryLinkSubnet;
        this.secondaryLinkSubnet = secondaryLinkSubnet;
        this.primaryLinkMac = primaryLinkMac;
        this.secondaryLinkMac = secondaryLinkMac;
        this.primaryLinkPort = primaryLinkPort;
        this.secondaryLinkPort = secondaryLinkPort;

    }

    public DeviceId getDeviceId() {
        return deviceId;
    }

    public Ip4Address getLoopbackIP() {
        return loopbackIP;
    }

    public Ip4Prefix getPrimaryLinkSubnet() {
        return primaryLinkSubnet;
    }

    public Ip4Prefix getSecondaryLinkSubnet() {
        return secondaryLinkSubnet;
    }

    public MacAddress getPrimaryLinkMac() {
        return primaryLinkMac;
    }

    public MacAddress getSecondaryLinkMac() {
        return secondaryLinkMac;
    }

    public PortNumber getPrimaryLinkPort() {
        return primaryLinkPort;
    }

    public PortNumber getSecondaryLinkPort() {
        return secondaryLinkPort;
    }


     public boolean equals(Object otherObject) {

        if(otherObject instanceof AggDeviceConfig) {

            AggDeviceConfig other = (AggDeviceConfig) otherObject;
            return deviceId.equals(other.deviceId) && loopbackIP.equals(other.loopbackIP) && primaryLinkSubnet.equals(other.primaryLinkSubnet) &&
                    secondaryLinkSubnet.equals(other.secondaryLinkSubnet) && primaryLinkPort.equals(other.primaryLinkPort) && secondaryLinkPort.equals(other.secondaryLinkPort) &&
                    primaryLinkMac.equals(other.primaryLinkMac) && secondaryLinkMac.equals(other.secondaryLinkMac);

        }

        return false;

    }
}
