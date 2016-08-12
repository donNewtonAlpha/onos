package org.onosproject.noviaggswitch.web;



import static org.slf4j.LoggerFactory.getLogger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

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
import org.onosproject.noviaggswitch.NoviAggSwitchComponent;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;

/**
 * REST services to interact with the aggregation switch.
 */
@Path("vxlanTunnel")
public class RestNoviAggSwitch extends AbstractWebResource {

    private final Logger log = getLogger(getClass());

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

            log.info("Vxlan tunnel requested for port : " +port + ", IP : " + vbngIP + ", vni : " + vni + ", viaIP : " + viaPrimaryIP.toString() + ", " + viaSecondaryIP.toString());

            try{
                Ip4Address.valueOf(viaPrimaryIP);
                Ip4Address.valueOf(viaSecondaryIP);
                NoviAggSwitchComponent.getComponent().addAccessDevice(port, vni, vbngIP, viaPrimaryIP, viaSecondaryIP);
            } catch(IllegalArgumentException e) {
                NoviAggSwitchComponent.getComponent().addAccessDevice(port, vni, vbngIP);
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


            log.info("Vxlan tunnel removal requested for IP : " + ip + ", vni : " + vni);

            try{
                Ip4Address vxlanIP = Ip4Address.valueOf(ip);
                NoviAggSwitchComponent.getComponent().removeTunnel(vxlanIP, vni);
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

            NoviAggSwitchComponent.getComponent().removeAllTunnels();

            return Response.ok().build();

        } catch (Exception e) {
            log.error("REST error", e);
        }

        return Response.status(406).build();
    }


    @GET
    @Path("test")
    @Produces(MediaType.APPLICATION_JSON)
    public Response test(InputStream stream) {


        return Response.ok().build();
    }

}
