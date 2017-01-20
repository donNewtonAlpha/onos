package org.onosproject.novibng;

import org.onlab.packet.*;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by nick on 8/8/16.
 */
public class NoviBngPacketProcessor implements PacketProcessor {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private PacketService packetService;


    private volatile List<MacRequest> macRequests = new LinkedList<>();
    private List<RoutingInfo> routingInfos = new LinkedList<>();
    private volatile boolean arpThreadActive;


    public NoviBngPacketProcessor(PacketService packetService) {
        this.packetService = packetService;
    }

    public void startARPingThread() {

        arpThreadActive = true;

        //Create thread that periodically make ARP request
        Runnable r = new Runnable() {
            @Override
            public void run() {

                while(arpThreadActive) {

                    if (macRequests.size() > 0) {

                        for (MacRequest request : macRequests) {
                            request.execute(packetService);
                        }
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        Thread t = new Thread(r);
        t.setDaemon(true);
        t.start();
    }

    public void stopARPingThread(){
        arpThreadActive = false;
    }


    @Override
    public void process(PacketContext context) {
        if (context.isHandled()) {
            return;
        }

        try {
            InboundPacket pkt = context.inPacket();


            Ethernet ethPkt = pkt.parsed();
            IPacket payload = ethPkt.getPayload();

            PortNumber inPort = pkt.receivedFrom().port();
            DeviceId deviceId = pkt.receivedFrom().deviceId();

            if (payload instanceof ARP) {

                log.debug("ARP packet received");

                ARP arp = (ARP) payload;

                if (arp.getOpCode() == ARP.OP_REQUEST) {
                    log.debug("It is an ARP request");
                    handleArpRequest(deviceId, inPort, ethPkt);
                } else {
                    log.debug("It is an ARP reply");
                    handleArpReply(deviceId, inPort, ethPkt);
                }
            } else if(payload instanceof IPv4) {

                log.debug("IP packet received");
                IPv4 ipPkt = (IPv4) payload;

                IPacket ipPayload = ipPkt.getPayload();

                if(ipPayload instanceof ICMP) {

                    log.debug("ICMP packet received");

                    ICMP ping = (ICMP) ipPayload;


                    if (ping.getIcmpType() == ICMP.TYPE_ECHO_REQUEST) {
                        log.info("ICMP request received");
                        handlePingRequest(deviceId, inPort, ethPkt);
                    } else if (ping.getIcmpType() == ICMP.TYPE_ECHO_REPLY) {
                            log.info("ICMP reply received");
                        //TODO
                    }
                } else if (ipPayload instanceof IGMP) {

                    log.info("IGMP packet received");
                    log.error("IGMP treatment not yet implemented");

                    //TODO:
                    //IGMPhandler.handlePacket(context);


                }
            }
        } catch(Exception e){
            log.error("Exception during processing" , e);
        }
    }


    private void handleArpRequest(DeviceId deviceId, PortNumber inPort, Ethernet ethPkt) {
        ARP arpRequest = (ARP) ethPkt.getPayload();
        Ip4Address targetProtocolAddress = Ip4Address.valueOf(
                arpRequest.getTargetProtocolAddress());
        log.info("ARP request for " + targetProtocolAddress + " on port " + inPort.toString() + " from " + Ip4Address.valueOf(
                arpRequest.getSenderProtocolAddress()));
        // Check if this is an ARP for the switch
        boolean matchingInfoFound = false;
        for(RoutingInfo info : routingInfos) {

            if(info.match(deviceId, inPort, targetProtocolAddress)) {
                matchingInfoFound = true;
                sendArpResponse(ethPkt, arpRequest, info.getMac(), info.getDeviceId(), inPort);
            }

        }

        if(!matchingInfoFound) {
            log.error("handleArpRequest, matching routing info was NOT FOUND");
        }

    }

    private void handleArpReply(DeviceId deviceId, PortNumber inPort, Ethernet ethPkt) {

        ARP arpReply = (ARP) ethPkt.getPayload();

        // ARP reply for router. Process all pending IP packets.
        Ip4Address hostIpAddress = Ip4Address.valueOf(arpReply.getSenderProtocolAddress());
        MacAddress mac = MacAddress.valueOf(arpReply.getSenderHardwareAddress());

        log.debug("ARP reply received from " + hostIpAddress + " : " + mac);


        for(MacRequest request : macRequests) {
            if(request.getIp().equals(hostIpAddress)) {
               request.success(mac);
            }
        }

    }

    private void handlePingRequest(DeviceId deviceId, PortNumber inPort, Ethernet ethPkt) {

        IPv4 ipPkt = (IPv4) ethPkt.getPayload();

        Ip4Address pingedIpAddress = Ip4Address.valueOf(ipPkt.getDestinationAddress());

        for(RoutingInfo info : routingInfos) {

            if(info.match(deviceId, inPort, pingedIpAddress)) {
                log.debug("This ping request is for this switch");
                sendICMPreply(ethPkt, info.getMac(), info.getDeviceId(), inPort);
            }

        }

    }

    private void sendICMPreply(Ethernet ethPkt, MacAddress mac, DeviceId deviceId, PortNumber port)  {

        log.debug("creation of the ICMP reply");

        IPv4 ipPkt = (IPv4) ethPkt.getPayload();
        ICMP ping = (ICMP) ipPkt.getPayload();

        ICMP pingResponse = new ICMP();
        pingResponse.setIcmpType(ICMP.TYPE_ECHO_REPLY);
        pingResponse.setIcmpCode(ICMP.SUBTYPE_ECHO_REPLY);
        pingResponse.setChecksum((short) 0);
        pingResponse.setPayload(ping.getPayload());

        IPv4 ipResponse = new IPv4();
        ipResponse.setDestinationAddress(ipPkt.getSourceAddress());
        ipResponse.setSourceAddress(ipPkt.getDestinationAddress());
        ipResponse.setTtl((byte) 64);
        ipResponse.setChecksum((short)0);

        ipResponse.setPayload(pingResponse);
        //pingResponse.setParent(ipResponse);


        //Modify the original request and send it back : It keeps the proper vlan tagging
        ethPkt.setDestinationMACAddress(ethPkt.getSourceMAC())
                .setSourceMACAddress(mac)
                .setEtherType(Ethernet.TYPE_IPV4)
                .setPayload(ipResponse);


        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.setOutput(port);
        OutboundPacket outPacket = new DefaultOutboundPacket(deviceId,
                treatment.build(), ByteBuffer.wrap(ethPkt.serialize()));

        packetService.emit(outPacket);
        log.info("ICMP reply response from " + Ip4Address.valueOf(ipPkt.getDestinationAddress()) + " to " + Ip4Address.valueOf(ipPkt.getSourceAddress()) + " sent on port " + port.toString());

    }



    public OutboundPacket getArpRequest(IpAddress targetAddress, IpAddress sourceAddress, MacAddress sourceMac, DeviceId deviceId, PortNumber dstPort, VlanId vlanId) {


        ARP arpRequest = new ARP();
        arpRequest.setHardwareType(ARP.HW_TYPE_ETHERNET)
                .setProtocolType(ARP.PROTO_TYPE_IP)
                .setHardwareAddressLength(
                        (byte) Ethernet.DATALAYER_ADDRESS_LENGTH)
                .setProtocolAddressLength((byte) Ip4Address.BYTE_LENGTH)
                .setOpCode(ARP.OP_REQUEST)
                .setSenderHardwareAddress(sourceMac.toBytes())
                .setTargetHardwareAddress(MacAddress.ZERO.toBytes())
                .setSenderProtocolAddress(sourceAddress.toOctets())
                .setTargetProtocolAddress(targetAddress.toOctets());

        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(MacAddress.BROADCAST.toBytes())
                .setSourceMACAddress(sourceMac)
                .setEtherType(Ethernet.TYPE_ARP).setPayload(arpRequest);

        if(!vlanId.equals(VlanId.NONE)){
            eth.setVlanID(vlanId.toShort());
        }

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.setOutput(dstPort);
        OutboundPacket outPacket = new DefaultOutboundPacket(deviceId,
                treatment.build(), ByteBuffer.wrap(eth.serialize()));

        return outPacket;
    }

    public void sendArpRequest(IpAddress targetAddress, IpAddress sourceAddress, MacAddress sourceMac, DeviceId deviceId, PortNumber dstPort, VlanId vlanId) {

        packetService.emit(getArpRequest(targetAddress, sourceAddress, sourceMac, deviceId, dstPort, vlanId));
    }

    private void sendArpResponse(Ethernet eth, ARP arpRequest, MacAddress mac, DeviceId deviceId, PortNumber dstPort) {

        //log.info("Preparing to send ARP response !!");

        ARP arpReply = new ARP();
        arpReply.setHardwareType(ARP.HW_TYPE_ETHERNET)
                .setProtocolType(ARP.PROTO_TYPE_IP)
                .setHardwareAddressLength(
                        (byte) Ethernet.DATALAYER_ADDRESS_LENGTH)
                .setProtocolAddressLength((byte) Ip4Address.BYTE_LENGTH)
                .setOpCode(ARP.OP_REPLY)
                .setSenderHardwareAddress(mac.toBytes())
                .setSenderProtocolAddress(arpRequest.getTargetProtocolAddress())
                .setTargetHardwareAddress(arpRequest.getSenderHardwareAddress())
                .setTargetProtocolAddress(arpRequest.getSenderProtocolAddress());


        //Modify the original request and send it back : It keeps the proper vlan tagging
        eth.setDestinationMACAddress(arpRequest.getSenderHardwareAddress())
                .setSourceMACAddress(mac)
                .setEtherType(Ethernet.TYPE_ARP)
                .setPayload(arpReply);

        //log.info("ARP integrated to Ethernet packet, ready to send");

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.setOutput(dstPort);
        OutboundPacket outPacket = new DefaultOutboundPacket(deviceId,
                treatment.build(), ByteBuffer.wrap(eth.serialize()));

        packetService.emit(outPacket);
        log.info("ARP response sent for IP : " + Ip4Address.valueOf(arpRequest.getTargetProtocolAddress()) + " on port " + dstPort.toString());
    }

    public void addRoutingInfo(DeviceId deviceId, PortNumber port, Ip4Prefix subnet, Ip4Address ip, MacAddress mac) {
        routingInfos.add(new RoutingInfo(deviceId, port, subnet, ip, mac));
    }

    private void removeRoutingInfo(RoutingInfo info) {
        routingInfos.remove(info);
    }

    public void clearRoutingInfo() {
        routingInfos.clear();
    }

    public void clearRoutingInfo(DeviceId deviceId) {

        Iterator<RoutingInfo> it = routingInfos.listIterator();

        while(it.hasNext()) {
            RoutingInfo info = it.next();
            if (info.getDeviceId().equals(deviceId)) {
                log.info("Remove one RoutingInfo , ip : " + info.getIp() + " for device " + deviceId);
                it.remove();
            }
        }
    }



    public MacAddress getMac(Ip4Address ip) {

        //Check if we already have some MacRequest for this Ip

        for(MacRequest request : macRequests) {

            if(request.getIp().equals(ip)) {
                //A request already exist, add this tunnelId to the requesting tunnels
                if(request.getMac() != null) {
                    return request.getMac();
                } else {
                    //This request is already trying to get the Mac address for this Ip, waiting for result
                    request.lock();

                    if(request.getMac() == null) {
                        return  null;
                    }

                    log.info("MAC found : " + request.getMac() + " for " + ip);
                    return request.getMac();

                }
            }

        }


        //Find matching routing info
        RoutingInfo matchingInfo = null;
        for(RoutingInfo info : routingInfos) {
            if(info.getSubnet().contains(ip)){
                matchingInfo = info;
            }
        }

        if(matchingInfo != null) {

            MacRequest request = new MacRequest(matchingInfo.getDeviceId(), matchingInfo.getPort(), ip);
            macRequests.add(request);

            OutboundPacket arpRequest = getArpRequest(ip, matchingInfo.getIp(), matchingInfo.getMac(), matchingInfo.getDeviceId(), matchingInfo.getPort(), VlanId.NONE);

            request.setArpRequest(arpRequest);
            request.execute(packetService);

            request.lock();

            if(request.getMac() == null) {
                return  null;
            }

            log.info("MAC found : " + request.getMac() + " for " + ip);


            return request.getMac();
        } else {
            log.error("No matching routing info for : " + ip.toString());
            return null;
        }


    }

    public void clearMacRequests() {
        for(MacRequest mr : macRequests) {
            mr.unlock();
        }
        macRequests.clear();
    }

    public void clearMacRequests(DeviceId deviceId) {

        Iterator<MacRequest> it = macRequests.listIterator();

        while(it.hasNext()) {
            MacRequest request = it.next();
            if(request.getDeviceId().equals(deviceId)) {
                request.unlock();
                it.remove();
            }

        }
    }



    private class RoutingInfo {

        private DeviceId deviceId;
        private PortNumber port;
        private Ip4Prefix subnet;
        private Ip4Address ip;
        private MacAddress mac;

        public RoutingInfo(DeviceId deviceId, PortNumber port, Ip4Prefix subnet, Ip4Address ip, MacAddress mac) {

            this.deviceId = deviceId;
            this.port = port;
            this.subnet = subnet;
            this.ip = ip;
            this.mac = mac;

        }

        public DeviceId getDeviceId() {
            return deviceId;
        }

        public PortNumber getPort() {
            return port;
        }

        public Ip4Prefix getSubnet() {
            return subnet;
        }

        public Ip4Address getIp() {
            return ip;
        }

        public MacAddress getMac() {
            return mac;
        }

        public boolean match(DeviceId deviceId, PortNumber port, Ip4Address ip) {

            boolean b = this.deviceId.equals(deviceId) && this.ip.equals(ip);

            if(!port.equals(PortNumber.ANY)) {
                b = b && this.port.equals(port);
            }

            return b;
        }

        public boolean equals(Object otherObject) {
            if (otherObject instanceof RoutingInfo) {
                RoutingInfo other = (RoutingInfo) otherObject;
                return deviceId.equals(other.getDeviceId()) && port.equals(other.getPort())
                        && subnet.equals(other.getSubnet()) && ip.equals(other.getIp()) && mac.equals(other.getMac());
            }

            return false;
        }
    }

}


