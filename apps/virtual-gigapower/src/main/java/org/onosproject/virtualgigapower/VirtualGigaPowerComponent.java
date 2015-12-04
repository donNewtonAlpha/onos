package org.onosproject.virtualgigapower;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.*;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.*;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.stream.JsonParser;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by nick on 10/12/15.
 */
public class VirtualGigaPowerComponent {


    static final Logger log = LoggerFactory.getLogger(VirtualGigaPowerComponent.class);

    private static final int JSON_COMMUNICATION_PORT = 6493;
    private static final String CONFIG_FILE = "";

    public static final int INTERNET_MPLS_LABEL = 404;



    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected static CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected static PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected static FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected static GroupService groupService;

    static ApplicationId appId;

    private List<Spine> spines = new LinkedList<>();
    private List<Leaf> leaves = new LinkedList<>();

    private FailureDetection failureDetection = new FailureDetection();

    private static int currentMPLS = 50;



    @Activate
    protected void activate() {

        log.debug("Activating Virtual GigaPower");
        appId = coreService.registerApplication("org.onosproject.virtualgigapower");

        //Check if there are config files
        try{
            //Read the config files
            JsonReader jsonReader = Json.createReader(new FileInputStream(CONFIG_FILE));
            JsonObject configs = jsonReader.readObject();

            //Apply the config
            applyConfig(configs);
        }catch (Exception e){
            log.error("Exception occured during config retrieval", e);
        }

        Fabric.getFabric().connectFabric();

    }

    // TODO
    private void applyConfig(JsonObject configs){}






}
