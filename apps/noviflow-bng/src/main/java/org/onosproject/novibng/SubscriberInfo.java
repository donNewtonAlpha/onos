package org.onosproject.novibng;

import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

/**
 * Created by nick on 1/19/17.
 */
public class SubscriberInfo {

    private PortNumber port;
    private VlanId cTag;
    private VlanId sTag;
    private int uploadSpeed;
    private int downloadSpeed;

    public SubscriberInfo() {
        this.port = null;
        this.cTag = null;
        this.sTag = null;
    }


    public PortNumber getPort() {
        return port;
    }

    public VlanId getCTag() {
        return cTag;
    }

    public VlanId getSTag() {
        return sTag;
    }

    public int getUploadSpeed() {
        return uploadSpeed;
    }

    public int getDownloadSpeed() {
        return downloadSpeed;
    }

    public void setUploadSpeed(int uploadSpeed) {
        this.uploadSpeed = uploadSpeed;
    }

    public void setDownloadSpeed(int downloadSpeed) {
        this.downloadSpeed = downloadSpeed;
    }

    public void setPort(PortNumber port) {
        this.port = port;
    }

    public void setCTag(VlanId cTag) {
        this.cTag = cTag;
    }

    public void setSTag(VlanId sTag) {
        this.sTag = sTag;
    }
}
