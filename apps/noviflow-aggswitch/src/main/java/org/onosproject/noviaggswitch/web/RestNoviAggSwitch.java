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

            log.info("Vxlan tunnel requested for port : " +port + ", IP : " + vbngIP + ", vni : " + vni);

            NoviAggSwitchComponent.getComponent().addAccessDevice(port, vni, vbngIP);

            return Response.ok().build();

        } catch (IOException e) {
            log.error("REST error", e);
        }

        return Response.status(409).build();
    }

    @GET
    @Path("test")
    @Produces(MediaType.APPLICATION_JSON)
    public Response test(InputStream stream) {


        return Response.ok().build();
    }

}
