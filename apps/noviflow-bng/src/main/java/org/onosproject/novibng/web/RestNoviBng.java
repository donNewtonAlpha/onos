package org.onosproject.novibng.web;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.onosproject.novibng.NoviBngComponent;
import org.onosproject.novibng.config.BngDeviceConfig;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * REST services to interact with the aggregation switch.
 */
@Path("bng")
public class RestNoviBng extends AbstractWebResource {

    private final Logger log = getLogger(getClass());

    private static final String LOOPBACK_IP = "loopbackIp";
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
    /*@POST
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
                NoviBngComponent.getComponent().addAccessDevice(deviceId, port, vni, vbngIP, viaPrimaryIP, viaSecondaryIP);
            } catch(IllegalArgumentException e) {
                log.error("REST API create tunnel error", e);
                return Response.status(406).build();
            }

            return Response.ok().build();

        } catch (IOException e) {
            log.error("REST error", e);
        }

        return Response.status(406).build();
    }*/


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

                final String[] partsPrimary = primaryLinkSubnet.split("/");
                final String[] partsSecondary = secondaryLinkSubnet.split("/");
                if (partsPrimary.length != 2 || partsSecondary.length != 2) {
                    String msg = "Malformed IPv4 prefix string: " + primaryLinkSubnet + " or " + secondaryLinkSubnet + ". " +
                            "Address must take form \"x.x.x.x/y\"";
                    throw new IllegalArgumentException(msg);
                }

                Ip4Address primaryIp = Ip4Address.valueOf(partsPrimary[0]);
                int primaryPrefixLength = Integer.parseInt(partsPrimary[1]);

                Ip4Address secondaryIp = Ip4Address.valueOf(partsSecondary[0]);
                int secondaryPrefixLength = Integer.parseInt(partsSecondary[1]);


                BngDeviceConfig newConfig = new BngDeviceConfig(DeviceId.deviceId(deviceUri), Ip4Address.valueOf(loopbackIp), primaryIp, primaryPrefixLength,
                        secondaryIp, secondaryPrefixLength, MacAddress.valueOf(primaryLinkMac), MacAddress.valueOf(secondaryLinkMac), PortNumber.portNumber(primaryPort), PortNumber.portNumber(secondaryPort));

                //TODO : check validity of config

                NoviBngComponent.getComponent().checkNewConfig(newConfig);
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
