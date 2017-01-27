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

package org.onosproject.novibng;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Semaphore;

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



    public void lock() {
        try {
            lock.acquire();
        } catch (Exception e) {
            log.error("Lock exception : ", e);
        }
    }

    public void unlock() {
        lock.release(50);
    }

    private void sendRequest(PacketService packetService) {
        failedAttempt++;
        packetService.emit(arpRequest);
        log.info("ARP request for " + ip.toString() + " sent");
    }


    public synchronized void execute(PacketService packetService) {

        if (arpRequest != null) {

            delay++;

            if (mac == null) {
                //Mac not yet known
                if (delay % period == 0) {
                    sendRequest(packetService);
                } else {
                    return;
                }

                if (failedAttempt % BACKOFF_PERIOD == 0) {
                    period = 1 + failedAttempt / BACKOFF_PERIOD;
                    log.warn(ip + " still not reachable on device " + deviceId + " on port " + port);
                }
            } else {
                //MAC already known, checking for reachability and changes

                if (delay > CYCLE) {
                    sendRequest(packetService);
                }

                if (failedAttempt > LOSS_BEFORE_FAILURE) {
                    if (!failureState) {
                        //Now in failure
                        NoviBngComponent.getComponent().notifyFailure(deviceId, port, mac);
                        failureState = true;
                    } else {
                        //Still in failure
                        if (failedAttempt % CYCLE == 0) {
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

        if (mac != null) {
            if (failedAttempt > LOSS_BEFORE_FAILURE) {
                //Recovery situation
                if (responseMac.equals(mac)) {
                    //Same mac
                    NoviBngComponent.getComponent().notifyRecovery(deviceId, port, mac);
                } else {
                    //change of mac after recovery
                    NoviBngComponent.getComponent().notifyRecovery(deviceId, port, mac, responseMac);
                    mac = responseMac;
                }
            } else {
                // Normal situation, chack if mac has changed
                if (!responseMac.equals(mac)) {

                    NoviBngComponent.getComponent().notifyMacChange(deviceId, port, mac, responseMac);
                    mac = responseMac;
                }
            }
        } else {


            mac = responseMac;
            unlock();
            log.info("requested MAC for " + ip + " found");
        }

        failedAttempt = 0;
        delay = 0;
        failureState = false;
    }





    public boolean equals(Object otherObject) {
        if (otherObject instanceof MacRequest) {
            MacRequest other = (MacRequest) otherObject;
            return ip.equals(other.getIp());
        }

        return false;
    }

    public int hashCode() {
        return ip.hashCode();
    }

}
