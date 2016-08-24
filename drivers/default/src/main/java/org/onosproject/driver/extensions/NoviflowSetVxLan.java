package org.onosproject.driver.extensions;

import com.google.common.base.MoreObjects;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;


import java.nio.ByteBuffer;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by nick on 4/5/16.
 */
public class NoviflowSetVxLan extends AbstractExtension implements ExtensionTreatment {

    private MacAddress dstMac;
    private MacAddress srcMac;
    private Ip4Address dstIp;
    private Ip4Address srcIp;
    private Integer udpPort;
    private Integer vXlanId;


    /**
     * Constructs a new set VxLAN instruction.
     */
    protected NoviflowSetVxLan() {

        dstMac = null;
        srcMac = null;
        dstIp = null;
        srcIp = null;
        udpPort = null;
        vXlanId = null;
    }

    /**
     * Constructs a new set VLAN ID instruction with given parameters.
     *
     */
    public NoviflowSetVxLan(MacAddress srcMac, MacAddress dstMac, Ip4Address srcIp,
                            Ip4Address dstIp, int udpPort, int vxlanId) {
        checkNotNull(srcMac);
        checkNotNull(dstMac);
        checkNotNull(srcIp);
        checkNotNull(dstIp);

        this.srcMac = srcMac;
        this.dstMac = dstMac;
        this.srcIp = srcIp;
        this.dstIp = dstIp;
        this.udpPort = udpPort;
        this.vXlanId = vxlanId;
    }

    public MacAddress getDstMac() {
        return dstMac;
    }

    public MacAddress getSrcMac() {
        return srcMac;
    }

    public Ip4Address getDstIp() {
        return dstIp;
    }

    public Ip4Address getSrcIp() {
        return srcIp;
    }

    public Integer getUdpPort() {
        return udpPort;
    }

    public Integer getVxLanId() {
        return vXlanId;
    }

    public void setDstMac(MacAddress newDstMac) {
        dstMac = newDstMac;
    }

    @Override
    public ExtensionTreatmentType type() {
        return ExtensionTreatmentType.ExtensionTreatmentTypes.NOVIFLOW_SET_VXLAN.type();
    }

    @Override
    public void deserialize(byte[] data) {

        byte[] bytesSrcMac = new byte[6];
        byte[] bytesDstMac = new byte[6];
        for (int i = 0; i < 6; i++) {
            bytesSrcMac[i] = data[i];
            bytesDstMac[i] = data[i + 6];
        }
        srcMac = MacAddress.valueOf(bytesSrcMac);
        dstMac =  MacAddress.valueOf(bytesDstMac);
        srcIp = Ip4Address.valueOf(bytesToInt(data, 12));
        dstIp = Ip4Address.valueOf(bytesToInt(data, 16));
        udpPort = bytesToInt(data, 20);
        vXlanId = bytesToInt(data, 24);

    }

    private int bytesToInt(byte[] bytes, int startIndex) {
        return bytes[startIndex] << 24 | (bytes[startIndex + 1] & 0xff) << 16 | (bytes[startIndex + 2] & 0xff) << 8
                | (bytes[startIndex + 3] & 0xff);
    }

    @Override
    public byte[] serialize() {

        ByteBuffer b = ByteBuffer.allocate(28);

        b.put(srcMac.toBytes());
        b.put(dstMac.toBytes());
        b.putInt(srcIp.toInt());
        b.putInt(dstIp.toInt());
        b.putInt(udpPort);
        b.putInt(vXlanId);

        return b.array();
    }

    @Override
    public int hashCode() {

        return Objects.hash(srcMac) * 17 + Objects.hash(dstMac) * 13 + Objects.hash(srcIp) * 11
                + Objects.hash(dstIp) * 7 + Objects.hash(udpPort) * 5 + Objects.hash(vXlanId) * 101;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NoviflowSetVxLan) {
            NoviflowSetVxLan that = (NoviflowSetVxLan) obj;
            return Objects.equals(srcMac, that.getSrcMac())
                    && Objects.equals(dstMac, that.getDstMac())
                    && Objects.equals(srcIp, that.getSrcIp())
                    && Objects.equals(dstIp, that.getDstIp())
                    && Objects.equals(udpPort, that.getUdpPort())
                    && Objects.equals(vXlanId, that.getVxLanId());

        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("srcMac", srcMac)
                .add("dstMac", dstMac)
                .add("srcIp", srcIp)
                .add("dstIp", dstIp)
                .add("Udp Port", udpPort)
                .add("VxLan id", vXlanId)
                .toString();
    }
}
