package org.onosproject.virtualgigapower;

import org.onlab.packet.Ip4Address;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by nick on 10/12/15.
 */
public class Leaf extends Switch{

    private List<SwitchingBehavior> personalBehaviors;
    private List<PortNumber> spineConnections;
    private int MplsTag;



    public Leaf(DeviceId spineId, Ip4Address ip, int id, String model){
        deviceId = spineId;
        managementIp = ip;
        this.id = id;
        hardwareModel = model;
        personalBehaviors = new LinkedList<>();
        spineConnections = new LinkedList<>();
    }

    public void addPersonnalBehavior(SwitchingBehavior newBehavior){
        personalBehaviors.add(newBehavior);
    }

    public void connectToSpine(int portNumber){
        spineConnections.add(PortNumber.portNumber(portNumber));
    }

    public void disconnectFromSpine(int portNumber){
        for (PortNumber port: spineConnections) {
            if(port.toLong() == portNumber){
                spineConnections.remove(port);
            }
        }
    }

    public void setMpls(int tag){
        this.MplsTag = tag;
    }

    public int getMplsLabel(){
        return MplsTag;
    }


}
