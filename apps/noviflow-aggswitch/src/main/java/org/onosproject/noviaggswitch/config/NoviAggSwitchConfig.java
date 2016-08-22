package org.onosproject.noviaggswitch.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
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
    private static final String DEVICES = "elements";


    @Override
    public boolean isValid() {

        boolean b = hasOnlyFields(DEVICES);
        List<ObjectNode> devices = devices();
        for(ObjectNode device: devices) {
            b = b && deviceIsValid(device);
        }

        return b;
    }

    private List<ObjectNode> devices() {

        ArrayNode jsonArray = (ArrayNode) object.path(DEVICES);
        List<ObjectNode> nodes = new LinkedList<>();
        for (JsonNode node : jsonArray) {
            nodes.add((ObjectNode)node);
        }

        return nodes;

    }

    private boolean deviceIsValid(ObjectNode device) {

        return primaryLinkIp(device) != null &&
                primaryLinkMac(device) != null &&
                primaryLinkPort(device) != null &&
                secondaryLinkIp(device) != null &&
                secondaryLinkMac(device) != null &&
                secondaryLinkPort(device) != null &&
                deviceId(device) != null;

    }

    public List<DeviceId> deviceIds() {

        List<DeviceId> deviceIds = new LinkedList<>();
        List<ObjectNode> devices = devices();

        for(ObjectNode device :  devices) {
            deviceIds.add(deviceId(device));
        }

        return deviceIds;
    }

    private ObjectNode getDeviceById(DeviceId deviceId) {

        List<ObjectNode> devices = devices();
        for(ObjectNode device : devices) {
            if(deviceId(device).equals(deviceId)) {
                return device;
            }
        }
        return null;

    }

    private Ip4Address loopbackIp(ObjectNode device){

        String ip = device.get(LOOPBACK_IP).asText("");
        Ip4Address loopbackIp = null;
        try{
            loopbackIp = Ip4Address.valueOf(ip);
        } catch(Exception e){
            log.debug("Invalid loopback Ip");
        }
        return loopbackIp;
    }

    private Ip4Prefix primaryLinkIp(ObjectNode device){
        String ip = device.get(PRIMARY_LINK_IP).asText("");
        Ip4Prefix primaryLinkIp = null;
        try{
            primaryLinkIp = Ip4Prefix.valueOf(ip);
        } catch(Exception e){
            log.debug("Invalid primary link Ip");
        }
        return primaryLinkIp;
    }

    private Ip4Prefix secondaryLinkIp(ObjectNode device){
        String ip = device.get(SECONDARY_LINK_IP).asText("");
        Ip4Prefix linkIp = null;
        try{
            linkIp = Ip4Prefix.valueOf(ip);
        } catch(Exception e){
            log.debug("Invalid secondary link Ip");
        }
        return linkIp;
    }

    private MacAddress primaryLinkMac(ObjectNode device){
        String mac = device.get(PRIMARY_LINK_MAC).asText("");
        MacAddress primaryLinkMac = null;
        try{
            primaryLinkMac = MacAddress.valueOf(mac);
        } catch(Exception e){
            log.debug("Invalid primary link mac");
        }
        return primaryLinkMac;
    }

    private MacAddress secondaryLinkMac(ObjectNode device){
        String mac = device.get(SECONDARY_LINK_MAC).asText("");
        MacAddress secondaryLinkMac = null;
        try{
            secondaryLinkMac = MacAddress.valueOf(mac);
        } catch(Exception e){
            log.debug("Invalid secondary link mac");
        }
        return secondaryLinkMac;
    }

    private PortNumber primaryLinkPort(ObjectNode device){
        int port = device.get(PRIMARY_LINK_PORT).asInt(0);
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

    private PortNumber secondaryLinkPort(ObjectNode device){
        int port = device.get(SECONDARY_LINK_PORT).asInt(0);
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

    private DeviceId deviceId(ObjectNode node){
        String device = node.get(DEVICE_ID).asText("");
        DeviceId deviceId = null;
        try{
            deviceId = DeviceId.deviceId(device);
        } catch(Exception e){
            log.debug("Invalid deviceId");
        }
        return deviceId;
    }

    public Ip4Address loopbackIp(DeviceId deviceId) {
        return loopbackIp(getDeviceById(deviceId));
    }

    public Ip4Prefix primaryLinkIp(DeviceId deviceId) {
        return primaryLinkIp(getDeviceById(deviceId));
    }

    public Ip4Prefix secondaryLinkIp(DeviceId deviceId) {
        return secondaryLinkIp(getDeviceById(deviceId));
    }

    public MacAddress primaryLinkMac(DeviceId deviceId) {
        return primaryLinkMac(getDeviceById(deviceId));
    }

    public MacAddress secondaryLinkMac(DeviceId deviceId) {
        return secondaryLinkMac(getDeviceById(deviceId));
    }

    public PortNumber primaryLinkPort(DeviceId deviceId) {
        return primaryLinkPort(getDeviceById(deviceId));
    }

    public PortNumber secondaryLinkPort(DeviceId deviceId) {
        return secondaryLinkPort(getDeviceById(deviceId));
    }

    public boolean hasChanged(NoviAggSwitchConfig oldConfig, DeviceId deviceId) {

        if(oldConfig == null) {
            return true;
        }

        ObjectNode newDevice = getDeviceById(deviceId);
        ObjectNode oldDevice = oldConfig.getDeviceById(deviceId);

        if(newDevice == null || oldDevice == null) {
            return true;
        }

        return !(loopbackIp(newDevice).equals(loopbackIp(oldDevice)) &&
                primaryLinkIp(newDevice).equals(primaryLinkIp(oldDevice)) &&
                secondaryLinkIp(newDevice).equals(secondaryLinkIp(oldDevice)) &&
                primaryLinkMac(newDevice).equals(primaryLinkMac(oldDevice)) &&
                secondaryLinkMac(newDevice).equals(secondaryLinkMac(oldDevice)) &&
                primaryLinkPort(newDevice).equals(primaryLinkPort(oldDevice)) &&
                secondaryLinkPort(newDevice).equals(secondaryLinkPort(oldDevice)));

    }



    public String toString(){

        StringBuilder builder = new StringBuilder();
        builder.append("Config is valid : "); builder.append(isValid());/*
        builder.append("; loopback Ip : "); builder.append(loopbackIp());
        builder.append(", primary link : "); *//*builder.append(primaryLinkIp());
        builder.append(" , ");*//* builder.append(primaryLinkMac());
        builder.append(" , port "); builder.append(primaryLinkPort());
        builder.append(", secondary link : ");*//* builder.append(secondaryLinkIp());
        builder.append(", ");*//* builder.append(secondaryLinkMac());
        builder.append(",  port "); builder.append(secondaryLinkPort());*/

        return builder.toString();
    }

    public void testConfig(){
        log.info("Testing the config, valid : " + isValid() +", " + toString());
    }





}
