package org.onosproject.noviaggswitch.web;



import static org.slf4j.LoggerFactory.getLogger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.noviaggswitch.AggDeviceConfig;
import org.onosproject.noviaggswitch.NoviAggSwitchComponent;
import org.onosproject.noviaggswitch.VxLanTunnel;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;

/**
 * REST services to interact with the aggregation switch.
 */
@Path("vxlanTunnel")
public class RestNoviAggSwitch extends AbstractWebResource {

    private final Logger log = getLogger(getClass());

    private static final String LOOPBACK_IP = "loopbakcIp";
    private static final String PRIMARY_LINK_IP = "primaryLinkIp";
    private static final String SECONDARY_LINK_IP = "secondaryLinkIp";
    private static final String PRIMARY_LINK_MAC = "primaryLinkMac";
    private static final String SECONDARY_LINK_MAC = "secondaryLinkMac";
    private static final String PRIMARY_LINK_PORT = "primaryLinkPort";
    private static final String SECONDARY_LINK_PORT = "secondaryLinkPort";
    private static final String DEVICE_ID = "deviceId";

    /**
     * Create a new virtual VxLAN tunnel.
     *
     * @return 200 ok
     */
    @POST
    @Path("createTunnel")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createVxlanTunnel(InputStream stream) {

        try {

            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            int port = jsonTree.findValue("port").asInt();
            int vni = jsonTree.findValue("vni").asInt();
            String vbngIP = jsonTree.findValue("vxlanIP").asText();
            String viaPrimaryIP = jsonTree.findValue("viaPrimaryIP").asText();
            String viaSecondaryIP = jsonTree.findValue("viaSecondaryIP").asText();
            String deviceUri = jsonTree.findValue("deviceId").asText();



            log.info("Vxlan tunnel requested for device " + deviceUri+ ", port : " +port + ", IP : " + vbngIP + ", vni : " + vni + ", viaIP : " + viaPrimaryIP + ", " + viaSecondaryIP);

            try{
                Ip4Address.valueOf(viaPrimaryIP);
                Ip4Address.valueOf(viaSecondaryIP);
                DeviceId deviceId = DeviceId.deviceId(deviceUri);
                NoviAggSwitchComponent.getComponent().addAccessDevice(deviceId, port, vni, vbngIP, viaPrimaryIP, viaSecondaryIP);
            } catch(IllegalArgumentException e) {
                log.error("REST API create tunnel error", e);
                return Response.status(406).build();
            }

            return Response.ok().build();

        } catch (IOException e) {
            log.error("REST error", e);
        }

        return Response.status(406).build();
    }


    @POST
    @Path("removeTunnel")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeTunnel(InputStream stream) {

        try {

            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);

            String ip = jsonTree.findValue("vxlanIP").asText();
            int vni = jsonTree.findValue("vni").asInt();
            String deviceUri = jsonTree.findValue("deviceId").asText();


            log.info("Vxlan tunnel removal requested for IP : " + ip + ", vni : " + vni);

            try{
                Ip4Address vxlanIP = Ip4Address.valueOf(ip);
                DeviceId deviceId = DeviceId.deviceId(deviceUri);
                NoviAggSwitchComponent.getComponent().removeTunnel(deviceId, vxlanIP, vni);
            } catch(IllegalArgumentException e) {
                return Response.status(406).build();
            }

            return Response.ok().build();

        } catch (IOException e) {
            log.error("REST error", e);
        }

        return Response.status(406).build();
    }
    @POST
    @Path("removeAllTunnels")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeAllTunnels(InputStream stream) {

        try {

            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            String deviceUri = jsonTree.findValue("deviceId").asText("");

            if(deviceUri.equals("")) {
                NoviAggSwitchComponent.getComponent().removeAllTunnels();
            } else {
                DeviceId deviceId = DeviceId.deviceId(deviceUri);
                NoviAggSwitchComponent.getComponent().removeAllTunnels(deviceId);
            }

            return Response.ok().build();

        } catch (Exception e) {
            log.error("REST error", e);
        }

        return Response.status(406).build();
    }


    @GET
    @Path("showTunnels")
    //@Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response test() {


        try {


            ObjectNode result = new ObjectMapper().createObjectNode();

            ArrayNode devices = result.putArray("devices");

            Set<DeviceId> aggDevices = NoviAggSwitchComponent.getComponent().getAggDevices();
            for(DeviceId deviceId : aggDevices) {



                ObjectNode device = new ObjectMapper().createObjectNode();
                device.put("deviceId", deviceId.toString());
                ArrayNode deviceTunnels = device.putArray("tunnels");

                List<VxLanTunnel> tunnels = NoviAggSwitchComponent.getComponent().getTunnels(deviceId);

                for (VxLanTunnel tunnel : tunnels) {
                    deviceTunnels.add(tunnel.jsonNode());
                }

                devices.add(device);

            }


            return Response.ok(result.toString()).build();

        } catch (Exception e) {
            log.error("REST error", e);
        }

        return Response.status(406).build();

    }

   /* @GET
    @Path("showTunnel")
    //@Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response test(InputStream stream) {


        try {

            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            String deviceUri = jsonTree.findValue("deviceId").asText("");

            ObjectNode result = new ObjectMapper().createObjectNode();

            ArrayNode devices = result.putArray("devices");

            boolean allDevices;
            DeviceId soleDevice = null;
            if(deviceUri.equals("")){
                //All devices
                allDevices = true;

            } else {
                soleDevice = DeviceId.deviceId(deviceUri);
                allDevices =false;
            }

            Set<DeviceId> aggDevices = NoviAggSwitchComponent.getComponent().getAggDevices();
            for(DeviceId deviceId : aggDevices) {

                if(allDevices || deviceId.equals(soleDevice)) {

                    ObjectNode device = new ObjectMapper().createObjectNode();
                    device.put("deviceId", deviceId.toString());
                    ArrayNode deviceTunnels = device.putArray("tunnels");

                    List<VxLanTunnel> tunnels = NoviAggSwitchComponent.getComponent().getTunnels(deviceId);

                    for (VxLanTunnel tunnel : tunnels) {
                        deviceTunnels.add(tunnel.jsonNode());
                    }

                    devices.add(device);
                }

            }


            return Response.ok(result.toString()).build();

        } catch (Exception e) {
            log.error("REST error", e);
        }

        return Response.status(406).build();

    }
*/
    /**
     * Add/modify the config for one device
     *
     * @return 200 ok
     */
    @POST
    @Path("config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response config(InputStream stream) {

        try {

            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);

            String deviceUri = jsonTree.findValue(DEVICE_ID).asText();
            String loopbackIp = jsonTree.findValue(LOOPBACK_IP).asText();
            String primaryLinkSubnet = jsonTree.findValue(PRIMARY_LINK_IP).asText();
            String secondaryLinkSubnet = jsonTree.findValue(SECONDARY_LINK_IP).asText();
            String primaryLinkMac = jsonTree.findValue(PRIMARY_LINK_MAC).asText();
            String secondaryLinkMac = jsonTree.findValue(SECONDARY_LINK_MAC).asText();
            int primaryPort = jsonTree.findValue(PRIMARY_LINK_PORT).asInt();
            int secondaryPort = jsonTree.findValue(SECONDARY_LINK_PORT).asInt();



            try{
                AggDeviceConfig newConfig = new AggDeviceConfig(DeviceId.deviceId(deviceUri), Ip4Address.valueOf(loopbackIp), Ip4Prefix.valueOf(primaryLinkSubnet),
                        Ip4Prefix.valueOf(secondaryLinkSubnet), MacAddress.valueOf(primaryLinkMac), MacAddress.valueOf(secondaryLinkMac), PortNumber.portNumber(primaryPort), PortNumber.portNumber(secondaryPort));

                //TODO : check validity of config

                NoviAggSwitchComponent.getComponent().checkNewConfig(newConfig);
            } catch(IllegalArgumentException e) {
                log.error("REST API config error, likely invalid config", e);
                return Response.status(406).entity("Invalid Config").build();
            }

            return Response.ok("Config for " + deviceUri + " added/modified").build();

        } catch (IOException e) {
            log.error("REST error", e);
        }

        return Response.status(406).build();
    }

}
