package org.onosproject.noviaggswitch;

import org.onlab.packet.*;
import org.onlab.packet.ipv6.Routing;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Created by nick on 8/8/16.
 */
public class NoviAggSwitchPacketProcessor implements PacketProcessor {

    private final Logger log = LoggerFactory.getLogger(getClass());

    PacketService packetService;


    List<MacRequest> macRequests = new LinkedList<>();
    List<RoutingInfo> routingInfos = new LinkedList<>();


    public NoviAggSwitchPacketProcessor(PacketService packetService) {
        this.packetService = packetService;

        //Create thread that periodically make ARP request
        Runnable r = new Runnable() {
            @Override
            public void run() {

                if(macRequests.size() > 0) {

                    for(MacRequest request: macRequests) {
                        request.execute();
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        Thread t = new Thread(r);
        t.setDaemon(true);
        t.start();

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

            if (payload instanceof ARP) {

                log.debug("ARP packet received");

                ARP arp = (ARP) payload;

                //TODO: pass deviceID info when multi switch
                PortNumber inPort = pkt.receivedFrom().port();

                if (arp.getOpCode() == ARP.OP_REQUEST) {
                    log.info("It is an ARP request");
                    handleArpRequest(inPort, ethPkt);
                } else {
                    log.debug("It is an ARP reply");
                    handleArpReply(inPort, ethPkt);
                }
            } else if(payload instanceof IPv4) {

                log.info("IP packet received");
                IPv4 ipPkt = (IPv4) payload;

                IPacket ipPayload = ipPkt.getPayload();

                if(ipPayload instanceof ICMP) {

                    log.info("ICMP packet received");

                    ICMP ping = (ICMP) ipPayload;

                    PortNumber inPort = pkt.receivedFrom().port();

                    if (ping.getIcmpType() == ICMP.TYPE_ECHO_REQUEST) {
                        log.info("It is a ping request");
                        handlePingRequest(inPort, ethPkt);
                    } else {
                        if (ping.getIcmpType() == ICMP.TYPE_ECHO_REPLY)
                            log.debug("It is a ping reply");
                        //TODO
                    }
                }
            }
        } catch(Exception e){
            log.error("Exception during processing" , e);
        }
    }


    private void handleArpRequest(PortNumber inPort, Ethernet ethPkt) {
        ARP arpRequest = (ARP) ethPkt.getPayload();
        Ip4Address targetProtocolAddress = Ip4Address.valueOf(
                arpRequest.getTargetProtocolAddress());
        log.info("ARP request for " + targetProtocolAddress + " on port " + inPort.toString());
        // Check if this is an ARP for the switch
        for(RoutingInfo info : routingInfos) {

            if(info.getPort().equals(inPort) && targetProtocolAddress.equals(info.getIp())){
                log.info("handleArpRequest, matching routing info found");
                sendArpResponse(ethPkt, arpRequest, info.getMac(), info.getDeviceId(), inPort);
            }

        }

    }

    private void handleArpReply(PortNumber inPort, Ethernet ethPkt) {
        ARP arpReply = (ARP) ethPkt.getPayload();
        log.info("ARP reply");


        // ARP reply for router. Process all pending IP packets.
        Ip4Address hostIpAddress = Ip4Address.valueOf(arpReply.getSenderProtocolAddress());
        for (MacRequest request : macRequests) {
            if (request.getIp().equals(hostIpAddress)) {
                MacAddress mac = MacAddress.valueOf(arpReply.getSenderHardwareAddress());

                request.setMac(mac);
                request.unlock();
                log.info("requested MAC for " + request.getIp() +" found");
                macRequests.remove(request);
            }
        }
    }

    private void handlePingRequest(PortNumber inPort, Ethernet ethPkt) {

        IPv4 ipPkt = (IPv4) ethPkt.getPayload();

        Ip4Address pingedIpAddress = Ip4Address.valueOf(ipPkt.getDestinationAddress());

        for(RoutingInfo info : routingInfos) {

            if(info.getPort().equals(inPort) && pingedIpAddress.equals(info.getIp())){
                log.info("This ping request is for this switch");
                sendICMPreply(ethPkt, info.getMac(), info.getDeviceId(), inPort);
            }

        }

    }

    private void sendICMPreply(Ethernet ethPkt, MacAddress mac, DeviceId deviceId, PortNumber port)  {

        log.info("creation of the ICMP reply");

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
        log.info("ICMP reply response sent");

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

        log.info("Preparing to send ARP response !!");

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
        log.info("ARP response sent");
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

    public MacAddress getMac(Ip4Address ip) {

        MacRequest request = new MacRequest(ip);
        macRequests.add(request);
        //Find matching routing info
        RoutingInfo matchingInfo = null;
        for(RoutingInfo info : routingInfos) {
            if(info.getSubnet().contains(ip)){
                matchingInfo = info;
            }
        }

        if(matchingInfo != null) {

            request.setArpRequest(getArpRequest(ip, matchingInfo.getIp(), matchingInfo.getMac(), matchingInfo.getDeviceId(), matchingInfo.getPort(), VlanId.NONE));
            request.execute();

            request.lock();

            log.info("MAC found : " + request.getMac() + " for " + ip);
            return request.getMac();
        } else {
            log.warn("No matching routing info for : " + ip.toString());
            return null;
        }


    }

    public void clearMacRequests() {
        for(MacRequest mr : macRequests) {
            mr.unlock();
        }
        macRequests.clear();
    }

    private class MacRequest {

        private Ip4Address ip;
        private Semaphore lock;
        private MacAddress matchingMac = null;

        private boolean needToARP = false;
        private OutboundPacket arpRequest = null;

        MacRequest(Ip4Address ip) {
            this.ip = ip;
            lock = new Semaphore(-1);
        }

        public void lock() {
            try{
                lock.acquire();
            } catch (Exception e) {
                log.error("Lock exception : " , e);
            }
        }

        public void unlock(){
            lock.release(2);
        }

        public void setMac(MacAddress mac) {
            matchingMac = mac;
            needToARP = false;
        }

        public MacAddress getMac() {
            return  matchingMac;
        }

        public Ip4Address getIp() {
            return ip;
        }

        public void setArpRequest(OutboundPacket request) {
            this.needToARP = true;
            this.arpRequest = request;
        }

        public void execute() {
            if(needToARP) {
                if(arpRequest != null){

                    packetService.emit(arpRequest);
                    log.info("ARP request for : " + ip.toString());

                } else {
                    log.warn("No ARP request assigned");
                }
            }
        }

        public boolean equals (Object otherObject) {
            if(otherObject instanceof MacRequest) {
                MacRequest other = (MacRequest) otherObject;
                return ip.equals(other.getIp());
            }

            return false;
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


