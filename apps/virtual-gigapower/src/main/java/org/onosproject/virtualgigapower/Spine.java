package org.onosproject.virtualgigapower;

import org.onlab.packet.Ip4Address;
import org.onosproject.net.DeviceId;

/**
 * Created by nick on 10/12/15.
 */
public class Spine extends Switch{


    private Leaf[] leavesConnected;


    public Spine(DeviceId spineId, Ip4Address ip, int id, String model, int numberOfPorts){
        deviceId = spineId;
        managementIp = ip;
        this.id = id;
        hardwareModel = model;
        leavesConnected = new Leaf[numberOfPorts];
    }


}
