package org.onosproject.noviaggswitch;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.packet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.Semaphore;
import java.util.List;
import java.util.LinkedList;

/**
 * Created by nick on 9/20/16.
 */
public class MacRequest {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final int BACKOFF_PERIOD = 30;
    private static final int LOSS_BEFORE_FAILURE = 5;
    private static final int CYCLE = 15;


    private DeviceId deviceId;
    private PortNumber port;
    private Ip4Address ip;
    private MacAddress mac = null;

    private List<VxlanTunnelId> tunnels;

    private Semaphore lock;

    private int failedAttempt;
    private int delay;
    private int period = 1;

    private OutboundPacket arpRequest = null;


    private boolean failureState = false;



    public MacRequest(DeviceId deviceId, PortNumber port, Ip4Address ip) {

        this.deviceId = deviceId;
        this.port = port;
        this.ip = ip;

        tunnels = new LinkedList<>();

        lock = new Semaphore(-1);

    }

    public Ip4Address getIp() {
        return ip;
    }

    public DeviceId getDeviceId() {
        return deviceId;
    }

    public MacAddress getMac() {
        return  mac;
    }

    public void setArpRequest(OutboundPacket request) {
        this.arpRequest = request;
    }

    public void setMac(MacAddress mac) {
        this.mac = mac;
    }

    public void addTunnelId (VxlanTunnelId tunnelId) {
        tunnels.add(tunnelId);
    }

    public boolean requestedByTunnel(VxlanTunnelId tunnelId) {

        for(VxlanTunnelId tId : tunnels) {
            if(tId.equals(tunnelId)){
                return true;
            }
        }
        return false;
    }

    public void removeRequestingTunnel(VxlanTunnelId tunnelId) {

        Iterator<VxlanTunnelId> it = tunnels.listIterator();
        while(it.hasNext()) {
            VxlanTunnelId tId = it.next();
            if(tId.equals(tunnelId)) {
                it.remove();
            }
        }
    }

    public boolean isToBeRemoved() {
        return tunnels.isEmpty();
    }


    public void lock() {
        try{
            lock.acquire();
        } catch (Exception e) {
            log.error("Lock exception : " , e);
        }
    }

    public void unlock(){
        lock.release(50);
    }

    private void sendRequest(PacketService packetService) {
        failedAttempt++;
        packetService.emit(arpRequest);
        log.info("ARP request for " + ip.toString() + " sent");
    }


    public synchronized void execute(PacketService packetService){

        if(arpRequest != null){

            delay ++;

            if(mac == null) {
                //Mac not yet known
                if (delay % period == 0) {
                    sendRequest(packetService);
                } else {
                    return;
                }

                if(failedAttempt % BACKOFF_PERIOD == 0) {
                    period = 1 + failedAttempt / BACKOFF_PERIOD;
                    log.warn(ip + " still not reachable on device " + deviceId + " on port " + port);
                }
            } else {
                //MAC already known, checking for reachability and changes

                if(delay > CYCLE) {
                    sendRequest(packetService);
                }

                if(failedAttempt > LOSS_BEFORE_FAILURE) {
                    if(!failureState) {
                        //Now in failure
                        NoviAggSwitchComponent.getComponent().notifyFailure(deviceId, port, mac);
                        failureState = true;
                    } else {
                        //Still in failure
                        if(failedAttempt % CYCLE == 0) {
                            log.warn(ip + " still not reachable on device " + deviceId + " on port " + port);
                        }
                    }
                }
            }


        } else {
            log.warn("No ARP request assigned");
        }

        delay++;

    }

    public synchronized void success(MacAddress responseMac) {

        log.info("Mac response success for " + ip + " : " + responseMac);

        if(mac != null) {
            if (failedAttempt > LOSS_BEFORE_FAILURE) {
                //Recovery situation
                if (responseMac.equals(mac)) {
                    //Same mac
                    NoviAggSwitchComponent.getComponent().notifyRecovery(deviceId, port, mac);
                } else {
                    //change of mac after recovery
                    NoviAggSwitchComponent.getComponent().notifyRecovery(deviceId, port, mac, responseMac);
                    mac = responseMac;
                }
            } else {
                // Normal situation, chack if mac has changed
                if (!responseMac.equals(mac)) {

                    NoviAggSwitchComponent.getComponent().notifyMacChange(deviceId, port, mac, responseMac);
                    mac = responseMac;
                }
            }
        } else {


            mac = responseMac;
            unlock();
            log.info("requested MAC for " + ip +" found");
        }

        failedAttempt = 0;
        delay = 0;
        failureState = false;
    }





    public boolean equals (Object otherObject) {
        if(otherObject instanceof MacRequest) {
            MacRequest other = (MacRequest) otherObject;
            return ip.equals(other.getIp());
        }

        return false;
    }



}
