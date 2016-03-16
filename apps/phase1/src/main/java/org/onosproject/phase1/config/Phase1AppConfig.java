package org.onosproject.phase1.config;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final String MULTICAST_VLAN = "multicastVlan";
    private static final String INTERNAL_INTERNET_VLAN = "internalInternetVlan";

    @Override
    public boolean isValid() {
        return hasOnlyFields(GATEWAY_IP,PRIMARY_UPLINK_FABRIC_SIDE_IP, SECONDARY_UPLINK_FABRIC_SIDE_IP,
                PRIMARY_UPLINK_UPLINK_SIDE_IP, SECONDARY_UPLINK_UPLINK_SIDE_IP, MULTICAST_VLAN, INTERNAL_INTERNET_VLAN)&&
                gatewayIp() != null &&
                primaryFabricIp() != null &&
                primaryUplinkIp() != null &&
                internalInternetVlan() != null;
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

    public String toString(){

        StringBuilder builder = new StringBuilder();
        builder.append("gateway Ip : "); builder.append(gatewayIp());
        builder.append(", primary uplink ip : "); builder.append(primaryUplinkIp());
        builder.append(" , primary fabric ip : "); builder.append(primaryFabricIp());
        builder.append(", secondary uplink ip : "); builder.append(secondaryUplinkIp());
        builder.append(", secondary fabric ip : "); builder.append(secondaryFabricIp());
        builder.append(", multicast vlan : "); builder.append(multicastVlan());
        builder.append(" internal internet vlan : "); builder.append(internalInternetVlan());

        return builder.toString();
    }

    public void testConfig(){
        log.info("Testing the config, valid : " + isValid() +", " + toString());
    }



}
