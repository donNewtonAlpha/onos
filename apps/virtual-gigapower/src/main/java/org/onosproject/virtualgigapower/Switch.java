package org.onosproject.virtualgigapower;

import org.onlab.packet.Ip4Address;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by nick on 10/12/15.
 */
public abstract class Switch {

    protected int id;
    protected Ip4Address managementIp;
    protected DeviceId deviceId;
    protected String hardwareModel;
    protected List<PortNumber> internetPorts = new LinkedList<>();

    public boolean hasInternetLink(){
        return !internetPorts.isEmpty();
    }

    public void addInternetLink(PortNumber port){
        internetPorts.add(port);
    }

    public List<PortNumber> getInternetPorts() {
        return internetPorts;
    }

    public DeviceId getDeviceId() {
        return deviceId;
    }

    public void removeInternetLink(PortNumber port){
        for(PortNumber portNumber: internetPorts){
            if(port.equals(portNumber)){
                internetPorts.remove(portNumber);

            }
        }
    }

}
