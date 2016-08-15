package org.onosproject.noviaggswitch;

import org.onlab.packet.*;
import org.onosproject.net.PortNumber;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.ConnectPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.LinkedList;

/**
 * Created by nick on 8/15/16.
 */
public class IGMPhandler {

    private static final Logger log = LoggerFactory.getLogger(IGMPhandler.class);

    public static void handlePacket(PacketContext context){


        try {
            InboundPacket pkt = context.inPacket();
            ConnectPoint from = pkt.receivedFrom();


            Ethernet ethPkt = pkt.parsed();

            IPv4 ipPkt = (IPv4) ethPkt.getPayload();

            IGMP igmpPacket = (IGMP) ipPkt.getPayload();

            List<IGMPGroup> igmpGroups = igmpPacket.getGroups();

            for(IGMPGroup group : igmpGroups) {

                if(group instanceof IGMPMembership) {

                    IGMPMembership membership = (IGMPMembership) group;
                    log.info("IGMP membership : " + membership.toString());

                    MulticastHandler mh = NoviAggSwitchComponent.getComponent().getMulticastHandler(from.deviceId());
                    //Lock to ensure thread safe behavior
                    mh.lock();

                    if(isAdditionQuery(membership)) {
                        log.info("It is an addition");
                        mh.addAccessNodeToFeed(membership.getGaddr().getIp4Address(), from.port());
                    } else {
                        log.info("It is a removal");
                        mh.removeAccessNodeToFeed(membership.getGaddr().getIp4Address(), from.port());
                    }

                    mh.unlock();

                }

            }


        } catch(Exception e){
            log.error("Exception during processing" , e);
        }



    }

    private static boolean isAdditionQuery(IGMPMembership membership) {

        if(membership.getRecordType() == IGMPMembership.MODE_IS_INCLUDE || membership.getRecordType() == IGMPMembership.CHANGE_TO_INCLUDE_MODE) {
            if(membership.getSources().size() > 0) {

                log.info("IGMP membership with INCLUDE or CHANGE_TO_INCLUDE and " + membership.getSources().size() + " sources");
                return true;

            } else {
                return false;
            }
        } else if(membership.getRecordType() == IGMPMembership.MODE_IS_EXCLUDE || membership.getRecordType() == IGMPMembership.CHANGE_TO_EXCLUDE_MODE) {

                log.info("IGMP membership with EXCLUDE or CHANGE_TO_EXCLUDE and " + membership.getSources().size() + " sources");
                return true;

        }

        return false;


    }


}


