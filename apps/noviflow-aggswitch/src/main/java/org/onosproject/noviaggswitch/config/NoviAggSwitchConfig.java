package org.onosproject.noviaggswitch.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
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

    private static final String GATEWAY_IP = "gatewayIp";
    private static final String PRIMARY_UPLINK_FABRIC_SIDE_IP= "fabricPrimaryUplinkIp";
    private static final String SECONDARY_UPLINK_FABRIC_SIDE_IP= "fabricSecondaryUplinkIp";
    private static final String PRIMARY_UPLINK_UPLINK_SIDE_IP= "primaryUplinkIp";
    private static final String SECONDARY_UPLINK_UPLINK_SIDE_IP= "secondaryUplinkIp";
    private static final String PRIMARY_UPLINK_PORT= "primaryUplinkPort";
    private static final String SECONDARY_UPLINK_PORT= "secondaryUplinkPort";
    private static final String MULTICAST_VLAN = "multicastVlan";
    private static final String INTERNAL_INTERNET_VLAN = "internalInternetVlan";
    private static final String VLAN_CROSSCONNECTS = "vlanCrossconnects";
    private static final String INTERNAL_WAN_PORTS = "internalWanPorts";

    private static final String VLAN = "vlan";
    private static final String PORTS = "ports";

    @Override
    public boolean isValid() {
        return hasOnlyFields(GATEWAY_IP,PRIMARY_UPLINK_FABRIC_SIDE_IP, SECONDARY_UPLINK_FABRIC_SIDE_IP,
                PRIMARY_UPLINK_UPLINK_SIDE_IP, SECONDARY_UPLINK_UPLINK_SIDE_IP, MULTICAST_VLAN,
                INTERNAL_INTERNET_VLAN, PRIMARY_UPLINK_PORT, SECONDARY_UPLINK_PORT, VLAN_CROSSCONNECTS, INTERNAL_WAN_PORTS)&&
                gatewayIp() != null &&
                primaryFabricIp() != null &&
                primaryUplinkIp() != null &&
                internalInternetVlan() != null &&
                primaryUplinkPort() != null &&
                secondaryUplinkPort() != null &&
                !internalWanPorts().isEmpty();
    }

    public Ip4Address gatewayIp(){
        String ip = get(GATEWAY_IP, "");
        Ip4Address gatewayIp = null;
        try{
            gatewayIp = Ip4Address.valueOf(ip);
        } catch(Exception e){
            log.debug("Invalid gateway Ip");
        }
        return gatewayIp;
    }

    public Ip4Address secondaryFabricIp(){
        String ip = get(SECONDARY_UPLINK_FABRIC_SIDE_IP, "");
        Ip4Address fabricIp = null;
        try{
            fabricIp = Ip4Address.valueOf(ip);
        } catch(Exception e){
            log.debug("Invalid secondary fabric Ip");
        }
        return fabricIp;
    }

    public Ip4Address primaryFabricIp(){
        String ip = get(PRIMARY_UPLINK_FABRIC_SIDE_IP, "");
        Ip4Address fabricIp = null;
        try{
            fabricIp = Ip4Address.valueOf(ip);
        } catch(Exception e){
            log.debug("Invalid primary fabric Ip");
        }
        return fabricIp;
    }

    public Ip4Address primaryUplinkIp(){
        String ip = get(PRIMARY_UPLINK_UPLINK_SIDE_IP, "");
        Ip4Address uplinkIp = null;
        try{
            uplinkIp = Ip4Address.valueOf(ip);
        } catch(Exception e){
            log.debug("Invalid promary uplink Ip");
        }
        return uplinkIp;
    }

    public Ip4Address secondaryUplinkIp(){
        String ip = get(SECONDARY_UPLINK_UPLINK_SIDE_IP, "");
        Ip4Address uplinkIp = null;
        try{
            uplinkIp = Ip4Address.valueOf(ip);
        } catch(Exception e){
            log.debug("Invalid secondary uplink Ip");
        }
        return uplinkIp;
    }

    public VlanId multicastVlan(){
        int vlan = get(MULTICAST_VLAN, -2);

        if(vlan == -1){
            return VlanId.NONE;
        }
        if(vlan < 0 || vlan > 4096){
            return null;
        }
        return VlanId.vlanId((short)vlan);
    }

    public VlanId internalInternetVlan(){
        int vlan = get(INTERNAL_INTERNET_VLAN, -2);

        if(vlan == -1){
            return VlanId.NONE;
        }
        if(vlan < 0 || vlan > 4096){
            return null;
        }
        return VlanId.vlanId((short)vlan);
    }

    public PortNumber primaryUplinkPort(){
        int port = get(PRIMARY_UPLINK_PORT, -1);
        if(port == -1){
            return null;
        } else {
            return PortNumber.portNumber(port);
        }
    }

    public PortNumber secondaryUplinkPort() {
        int port = get(SECONDARY_UPLINK_PORT, -1);
        if(port == -1){
            return null;
        } else {
            return PortNumber.portNumber(port);
        }
    }


    public List<PortNumber> internalWanPorts(){

        List<PortNumber> ports = new LinkedList<>();
        ArrayNode jsonArray = (ArrayNode) object.path(INTERNAL_WAN_PORTS);

        for(JsonNode portNode : jsonArray) {
            try {
                int portNumber = portNode.asInt(-2);
                if(portNumber != -2){
                    PortNumber port = PortNumber.portNumber(portNumber);
                    ports.add(port);
                } else {
                    log.warn("Invalid port number");
                }
            } catch (Exception e){
                log.error("Parsing exception, port numbers", e);
            }
        }

        return ports;
    }




    public String toString(){

        StringBuilder builder = new StringBuilder();
        builder.append("Config is valid : "); builder.append(isValid());
        builder.append("; gateway Ip : "); builder.append(gatewayIp());
        builder.append(", primary uplink ip : "); builder.append(primaryUplinkIp());
        builder.append(" , primary fabric ip : "); builder.append(primaryFabricIp());
        builder.append(", secondary uplink ip : "); builder.append(secondaryUplinkIp());
        builder.append(", secondary fabric ip : "); builder.append(secondaryFabricIp());
        builder.append(", multicast vlan : "); builder.append(multicastVlan());
        builder.append(", internal internet vlan : "); builder.append(internalInternetVlan());
        builder.append(", primary uplink port : "); builder.append(primaryUplinkPort());
        builder.append(", secondary uplink port : "); builder.append(secondaryUplinkPort());
        builder.append(", vlan crossconects : ");

        builder.append(", internalWanPorts : [");
        for(PortNumber port : internalWanPorts()) {
            builder.append(port);
            builder.append(", ");
        }
        builder.append("]");
        return builder.toString();
    }

    public void testConfig(){
        log.info("Testing the config, valid : " + isValid() +", " + toString());
    }





}
