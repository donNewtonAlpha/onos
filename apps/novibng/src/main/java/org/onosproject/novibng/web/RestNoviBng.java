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

package org.onosproject.novibng.web;


import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
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
    private static final String PRIMARY_NEXT_HOP_IP = "primaryNextHopIp";
    private static final String SECONDARY_NEXT_HOP_IP = "secondaryNextHopIp";
    private static final String GATEWAY_IP = "gatewayIp";
    private static final String GATEWAY_MAC = "gatewayMac";
    private static final String IP_BLOCK = "ipBlock";
    private static final String SUBSCRIBER_IP = "subscriberIp";
    private static final String UPLOAD_SPEED = "uploadSpeed";
    private static final String DOWNLOAD_SPEED = "downstreamSpeed";



    @GET
    @Path("test")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    /**
     * test REST interface
     *
     *
     * @return 200 ok
     */
    public Response test(InputStream stream) {

        return Response.ok().build();

    }

    @POST
    @Path("allocateIpBlock")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    /**
     * Allocate a new Ip block.
     *
     *
     * @return 200 ok
     */
    public Response allocateIpBlock(InputStream stream) {

        try {

            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);

            String ipBlockString = jsonTree.findValue(IP_BLOCK).asText();
            String gatewayIpString = jsonTree.findValue(GATEWAY_IP).asText();
            String gatewayMacString = jsonTree.findValue(GATEWAY_MAC).asText();
            String deviceUri = jsonTree.findValue(DEVICE_ID).asText();



            log.info("Ip block allocation requested for device " + deviceUri + ", ip block : " + ipBlockString + "," +
                    " gateway : " + gatewayIpString + " (" + gatewayMacString + ")");

            try {
                Ip4Address gatewayIp = Ip4Address.valueOf(gatewayIpString);
                MacAddress gatewayMac = MacAddress.valueOf(gatewayMacString);
                Ip4Prefix ipBlock = Ip4Prefix.valueOf(ipBlockString);
                DeviceId deviceId = DeviceId.deviceId(deviceUri);

                NoviBngComponent.getComponent().allocateIpBlock(deviceId, ipBlock, gatewayIp, gatewayMac);

            } catch (IllegalArgumentException e) {
                log.error("REST API allocate ip block error", e);
                return Response.status(406).build();
            }

            return Response.ok().build();

        } catch (IOException e) {
            log.error("REST error", e);
        }

        return Response.status(406).build();
    }


    @POST
    @Path("addSubscriber")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    /**
     * Provision a subscriber.
     *
     *
     * @return 200 ok
     */
    public Response provisionSubscriber(InputStream stream) {

        try {

            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);

            String subscriberIpString = jsonTree.findValue(SUBSCRIBER_IP).asText();
            String gatewayIpString = jsonTree.findValue(GATEWAY_IP).asText();
            String deviceUri = jsonTree.findValue(DEVICE_ID).asText();
            int uploadSpeed = jsonTree.findValue(UPLOAD_SPEED).asInt();
            int downloadSpeed = jsonTree.findValue(DOWNLOAD_SPEED).asInt();



            log.info("subscriber provisioning requested for device " + deviceUri + ", ip : " + subscriberIpString + ","
                    + " gateway : " + gatewayIpString + ", speed " + uploadSpeed + " kbps up and " + downloadSpeed +
                    " kbps down");

            try {
                Ip4Address gatewayIp = Ip4Address.valueOf(gatewayIpString);
                Ip4Address subscriberIp = Ip4Address.valueOf(subscriberIpString);
                DeviceId deviceId = DeviceId.deviceId(deviceUri);

                NoviBngComponent.getComponent().addSubscriber(subscriberIp, gatewayIp,
                        uploadSpeed, downloadSpeed, deviceId);

            } catch (IllegalArgumentException e) {
                log.error("REST API allocate ip block error", e);
                return Response.status(406).build();
            }

            return Response.ok().build();

        } catch (IOException e) {
            log.error("REST error", e);
        }

        return Response.status(406).build();
    }



    @POST
    @Path("config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    /**
     * Add/modify the config for one device.
     *
     *
     * @return 200 ok
     *
     */
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
            String primaryNextHopString = jsonTree.findValue(PRIMARY_NEXT_HOP_IP).asText();
            String secondaryNextHopString = jsonTree.findValue(SECONDARY_NEXT_HOP_IP).asText();



            try {

                final String[] partsPrimary = primaryLinkSubnet.split("/");
                final String[] partsSecondary = secondaryLinkSubnet.split("/");
                if (partsPrimary.length != 2 || partsSecondary.length != 2) {
                    String msg = "Malformed IPv4 prefix string: " + primaryLinkSubnet + " or "
                            + secondaryLinkSubnet + ". " + "Address must take form \"x.x.x.x/y\"";
                    throw new IllegalArgumentException(msg);
                }

                Ip4Address primaryIp = Ip4Address.valueOf(partsPrimary[0]);
                int primaryPrefixLength = Integer.parseInt(partsPrimary[1]);

                Ip4Address secondaryIp = Ip4Address.valueOf(partsSecondary[0]);
                int secondaryPrefixLength = Integer.parseInt(partsSecondary[1]);

                Ip4Address primaryNextHopIp = Ip4Address.valueOf(primaryNextHopString);
                Ip4Address secondaryNextHopIp = Ip4Address.valueOf(secondaryNextHopString);


                BngDeviceConfig newConfig = new BngDeviceConfig(DeviceId.deviceId(deviceUri),
                        Ip4Address.valueOf(loopbackIp), primaryIp, primaryPrefixLength, secondaryIp,
                        secondaryPrefixLength, MacAddress.valueOf(primaryLinkMac), MacAddress.valueOf(secondaryLinkMac),
                        PortNumber.portNumber(primaryPort), PortNumber.portNumber(secondaryPort), primaryNextHopIp,
                        secondaryNextHopIp);

                //TODO : check validity of config

                NoviBngComponent.getComponent().checkNewConfig(newConfig);
            } catch (IllegalArgumentException e) {
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
