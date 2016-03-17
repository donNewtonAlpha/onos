package org.onosproject.phase1.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by nick on 3/14/16.
 */
public class Phase1AppConfig extends Config<ApplicationId>{

    private static Logger log = LoggerFactory.getLogger(Phase1AppConfig.class);

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

    private static final String VLAN = "vlan";
    private static final String PORTS = "ports";

    @Override
    public boolean isValid() {
        return hasOnlyFields(GATEWAY_IP,PRIMARY_UPLINK_FABRIC_SIDE_IP, SECONDARY_UPLINK_FABRIC_SIDE_IP,
                PRIMARY_UPLINK_UPLINK_SIDE_IP, SECONDARY_UPLINK_UPLINK_SIDE_IP, MULTICAST_VLAN,
                INTERNAL_INTERNET_VLAN, PRIMARY_UPLINK_PORT, SECONDARY_UPLINK_PORT, VLAN_CROSSCONNECTS)&&
                gatewayIp() != null &&
                primaryFabricIp() != null &&
                primaryUplinkIp() != null &&
                internalInternetVlan() != null &&
                primaryUplinkPort() != null &&
                secondaryUplinkPort() != null;
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
        int vlan = get(MULTICAST_VLAN, -2);

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

    public List<VlanCrossconnect> vlanCrossconnects() {

        ArrayNode jsonArray = (ArrayNode) object.path(VLAN_CROSSCONNECTS);
        List<VlanCrossconnect> vlanCrossconnects = new LinkedList<>();
        for (JsonNode node : jsonArray) {
            try {
                //Each node contains a vlan croosconnect
                //Used to check validity of config
                boolean validConfig = true;

                int vlan = node.path(VLAN).asInt(-2);
                VlanId vlanId;

                if (vlan == -1) {
                    vlanId = VlanId.NONE;
                } else if (vlan > 4096 || vlan < 0) {
                    log.warn("Invalid Vlan provided");
                    validConfig = false;
                    vlanId = null;
                } else {
                    vlanId = VlanId.vlanId((short) vlan);
                }

                ArrayNode vlansArray = (ArrayNode) node.path(PORTS);
                int i = 0;
                int port1 = -1;
                int port2 = -1;
                for (JsonNode portNode : vlansArray) {
                    //Extract the 2 ports from the config
                    i++;
                    if(i == 1){
                        port1 = portNode.asInt(-1);
                    } else if (i == 2) {
                        port2 = portNode.asInt(-1);
                    }
                }

                //Check for validity of the config

                if(i != 2) {
                    log.warn("invalid number of ports");
                    validConfig = false;
                }

                if(port1 == -1 || port2 == -1) {
                    log.warn("invalid ports");
                    validConfig = false;
                }

                if(validConfig){
                    vlanCrossconnects.add(new VlanCrossconnect(vlanId, port1, port2));
                }


            } catch (Exception e) {
                log.error("Parsing exception", e);
            }
        }

        return vlanCrossconnects;
    }



    public String toString(){

        StringBuilder builder = new StringBuilder();
        builder.append("gateway Ip : "); builder.append(gatewayIp());
        builder.append(", primary uplink ip : "); builder.append(primaryUplinkIp());
        builder.append(" , primary fabric ip : "); builder.append(primaryFabricIp());
        builder.append(", secondary uplink ip : "); builder.append(secondaryUplinkIp());
        builder.append(", secondary fabric ip : "); builder.append(secondaryFabricIp());
        builder.append(", multicast vlan : "); builder.append(multicastVlan());
        builder.append(", internal internet vlan : "); builder.append(internalInternetVlan());
        builder.append(", primary uplink port : "); builder.append(primaryUplinkPort());
        builder.append(", secondary uplink port : "); builder.append(secondaryUplinkPort());
        builder.append(", vlan crossconects : ");
        for(VlanCrossconnect vlanCrossconnect : vlanCrossconnects()){
            builder.append(vlanCrossconnect);
            builder.append("; ");
        }

        return builder.toString();
    }

    public void testConfig(){
        log.info("Testing the config, valid : " + isValid() +", " + toString());
    }



}
