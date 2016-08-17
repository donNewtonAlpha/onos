package org.onosproject.noviaggswitch.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.basics.SubjectFactories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by nick on 8/9/16.
 */
public class NoviAggSwitchConfig extends Config<ApplicationId> {

    private Logger log = LoggerFactory.getLogger(getClass());

    private static final String LOOPBACK_IP = "loopbakcIp";
    private static final String PRIMARY_LINK_IP = "primaryLinkIp";
    private static final String SECONDARY_LINK_IP = "secondaryLinkIp";
    private static final String PRIMARY_LINK_MAC = "primaryLinkMac";
    private static final String SECONDARY_LINK_MAC = "secondaryLinkMac";
    private static final String PRIMARY_LINK_PORT = "primaryLinkPort";
    private static final String SECONDARY_LINK_PORT = "secondaryLinkPort";
    private static final String DEVICE_ID = "deviceId";


    @Override
    public boolean isValid() {
        return hasOnlyFields(LOOPBACK_IP, PRIMARY_LINK_IP, SECONDARY_LINK_IP, PRIMARY_LINK_MAC, SECONDARY_LINK_MAC,
                PRIMARY_LINK_PORT, SECONDARY_LINK_PORT, DEVICE_ID)&&
                primaryLinkIp() != null &&
                primaryLinkMac() != null &&
                primaryLinkPort() != null &&
                secondaryLinkIp() != null &&
                secondaryLinkMac() != null &&
                secondaryLinkPort() != null &&
                deviceId() != null;
    }

    public Ip4Address loopbackIp(){
        String ip = get(LOOPBACK_IP, "");
        Ip4Address loopbackIp = null;
        try{
            loopbackIp = Ip4Address.valueOf(ip);
        } catch(Exception e){
            log.debug("Invalid loopback Ip");
        }
        return loopbackIp;
    }

    public Ip4Address primaryLinkIp(){
        String ip = get(PRIMARY_LINK_IP, "");
        Ip4Address primaryLinkIp = null;
        try{
            primaryLinkIp = Ip4Address.valueOf(ip);
        } catch(Exception e){
            log.debug("Invalid primary link Ip");
        }
        return primaryLinkIp;
    }

    public Ip4Address secondaryLinkIp(){
        String ip = get(SECONDARY_LINK_IP, "");
        Ip4Address linkIp = null;
        try{
            linkIp = Ip4Address.valueOf(ip);
        } catch(Exception e){
            log.debug("Invalid secondary link Ip");
        }
        return linkIp;
    }

    public MacAddress primaryLinkMac(){
        String mac = get(PRIMARY_LINK_MAC, "");
        MacAddress primaryLinkMac = null;
        try{
            primaryLinkMac = MacAddress.valueOf(mac);
        } catch(Exception e){
            log.debug("Invalid primary link mac");
        }
        return primaryLinkMac;
    }

    public MacAddress secondaryLinkMac(){
        String mac = get(SECONDARY_LINK_MAC, "");
        MacAddress secondaryLinkMac = null;
        try{
            secondaryLinkMac = MacAddress.valueOf(mac);
        } catch(Exception e){
            log.debug("Invalid secondary link mac");
        }
        return secondaryLinkMac;
    }

    public PortNumber primaryLinkPort(){
        int port = get(PRIMARY_LINK_PORT, 0);
        PortNumber link = null;
        try{
            link = PortNumber.portNumber(port);
        } catch(Exception e){
            log.debug("Invalid primary link port");
        }
        if(port != 0) {
            return link;
        } else {
            return null;
        }
    }

    public PortNumber secondaryLinkPort(){
        int port = get(SECONDARY_LINK_PORT, 0);
        PortNumber link = null;
        try{
            link = PortNumber.portNumber(port);
        } catch(Exception e){
            log.debug("Invalid secondary link port");
        }
        if(port != 0) {
            return link;
        } else {
            return null;
        }
    }

    public DeviceId deviceId(){
        String device = get(DEVICE_ID, "");
        DeviceId deviceId = null;
        try{
            deviceId = DeviceId.deviceId(device);
        } catch(Exception e){
            log.debug("Invalid deviceId");
        }
        return deviceId;
    }




    public String toString(){

        StringBuilder builder = new StringBuilder();
        builder.append("Config is valid : "); builder.append(isValid());
        builder.append("; loopback Ip : "); builder.append(loopbackIp());
        builder.append(", primary link : "); /*builder.append(primaryLinkIp());
        builder.append(" , ");*/ builder.append(primaryLinkMac());
        builder.append(" , port "); builder.append(primaryLinkPort());
        builder.append(", secondary link : ");/* builder.append(secondaryLinkIp());
        builder.append(", ");*/ builder.append(secondaryLinkMac());
        builder.append(",  port "); builder.append(secondaryLinkPort());

        return builder.toString();
    }

    public void testConfig(){
        log.info("Testing the config, valid : " + isValid() +", " + toString());
    }





}
