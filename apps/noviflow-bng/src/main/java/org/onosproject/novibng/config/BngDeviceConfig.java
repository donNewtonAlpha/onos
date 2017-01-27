package org.onosproject.novibng.config;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by nick on 8/22/16.
 */
public class BngDeviceConfig {

    private Logger log = LoggerFactory.getLogger(getClass());

    private DeviceId deviceId;
    private Ip4Address loopbackIP;
    private Ip4Address primaryLinkIp;
    private int primaryLinkSubnetLength;
    private Ip4Address secondaryLinkIp;
    private int secondaryLinkSubnetLength;
    private MacAddress primaryLinkMac;
    private MacAddress secondaryLinkMac;
    private PortNumber primaryLinkPort;
    private PortNumber secondaryLinkPort;
    private Ip4Address primaryNextHopIp;
    private Ip4Address secondaryNextHopIp;

    public BngDeviceConfig(DeviceId deviceId, Ip4Address loopbackIP, Ip4Address primaryLinkIp,
                           int primaryLinkSubnetLength, Ip4Address secondaryLinkIp, int secondaryLinkSubnetLength,
                           MacAddress primaryLinkMac, MacAddress secondaryLinkMac, PortNumber primaryLinkPort,
                           PortNumber secondaryLinkPort, Ip4Address primaryNextHopIp, Ip4Address secondaryNextHopIp) {

        this.deviceId = deviceId;
        this.loopbackIP = loopbackIP;
        this.primaryLinkIp = primaryLinkIp;
        this.primaryLinkSubnetLength = primaryLinkSubnetLength;
        this.secondaryLinkIp = secondaryLinkIp;
        this.secondaryLinkSubnetLength = secondaryLinkSubnetLength;
        this.primaryLinkMac = primaryLinkMac;
        this.secondaryLinkMac = secondaryLinkMac;
        this.primaryLinkPort = primaryLinkPort;
        this.secondaryLinkPort = secondaryLinkPort;
        this.primaryNextHopIp = primaryNextHopIp;
        this.secondaryNextHopIp = secondaryNextHopIp;

    }

    public DeviceId getDeviceId() {
        return deviceId;
    }

    public Ip4Address getLoopbackIP() {
        return loopbackIP;
    }

    public Ip4Prefix getPrimaryLinkSubnet() {
        return Ip4Prefix.valueOf(primaryLinkIp, primaryLinkSubnetLength);
    }

    public Ip4Prefix getSecondaryLinkSubnet() {
        return Ip4Prefix.valueOf(secondaryLinkIp, secondaryLinkSubnetLength);
    }

    public Ip4Address getPrimaryLinkIp() {
        return primaryLinkIp;
    }

    public Ip4Address getSecondaryLinkIp() {
        return secondaryLinkIp;
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

    public Ip4Address getPrimaryNextHopIp() {
        return primaryNextHopIp;
    }

    public Ip4Address getSecondaryNextHopIp() {
        return secondaryNextHopIp;
    }

    public boolean equals(Object otherObject) {

        if(otherObject instanceof BngDeviceConfig) {

            BngDeviceConfig other = (BngDeviceConfig) otherObject;
            return deviceId.equals(other.deviceId) && loopbackIP.equals(other.loopbackIP) && primaryLinkIp.equals(other.primaryLinkIp) &&
                    secondaryLinkIp.equals(other.secondaryLinkIp) && primaryLinkSubnetLength == other.primaryLinkSubnetLength &&
                    secondaryLinkSubnetLength == other.secondaryLinkSubnetLength && primaryLinkPort.equals(other.primaryLinkPort) &&
                    secondaryLinkPort.equals(other.secondaryLinkPort) &&
                    primaryLinkMac.equals(other.primaryLinkMac) && secondaryLinkMac.equals(other.secondaryLinkMac);

        }

        return false;

    }
}