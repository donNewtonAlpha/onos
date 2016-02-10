package org.onosproject.phase1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.MacAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Created by nick on 11/9/15.
 */

@Path("tor")
public class TorRest {

    private static final Logger log =  LoggerFactory.getLogger(Phase1Component.class);



    Phase1Component tor = new Phase1Component();

    @POST
    @Path("action")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response action(InputStream is) throws IOException {
        log.info("Endpoint reached");
    try{
        JsonObject message = (JsonObject) extractJson(is);

        log.info("Json received : " + message.toString());

        tor.treatJson(message);

    }catch(Exception e){
        log.error("treatment of JSON exception", e);
        return Response.status(Response.Status.BAD_REQUEST).build();
    }


        return Response.ok().build();
    }



    public static JsonStructure extractJson(InputStream is) throws Exception{

        StringBuilder builder = new StringBuilder();
        StringBuilder msgBuilder = new StringBuilder();
        int next;
        boolean jsonContent = false;
        boolean b = true;
        int opened = 0;
        char endChar = ' ';
        char openChar =' ';
        while (b) {
            next = is.read();
            char c = (char) next;
            builder.append(c);
            if (jsonContent) {
                msgBuilder.append(c);
                if(c == openChar){
                    opened++;
                }
                if(c == endChar){
                    opened--;
                    if(opened == 0){
                        b = false;
                    }
                }
            }else{
                if (c == '{' || c == '[') {
                    jsonContent = true;
                    msgBuilder.append(c);
                    if(c == '{'){
                        openChar = '{';
                        endChar = '}';
                    }else{
                        openChar = '[';
                        endChar = ']';
                    }
                    opened++;
                }
            }
        }

        log.info("Json string : " + msgBuilder.toString());
        JsonReader reader = Json.createReader(new StringReader(msgBuilder.toString()));
        JsonStructure message = reader.read();
        reader.close();
        return message;
    }
}
