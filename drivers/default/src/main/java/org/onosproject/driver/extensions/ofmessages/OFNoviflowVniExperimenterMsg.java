package org.onosproject.driver.extensions.ofmessages;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowController;
import org.onosproject.openflow.controller.ThirdPartyMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.types.U16;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.types.U8;
import org.onosproject.net.DeviceId;



/**
 * Created by nick on 7/18/16.
 */
public class OFNoviflowVniExperimenterMsg extends ThirdPartyMessage {

    int tableId;

    public OFNoviflowVniExperimenterMsg(int tableId) {
        super(new byte[0]);
        this.tableId = tableId;
    }

    public OFNoviflowVniExperimenterMsg(byte[] payLoad) {
        super(payLoad);
    }

    @Override
    public void writeTo(ChannelBuffer channelBuffer) {
        //Experimenter
        channelBuffer.writeInt(U32.t(0xff000002));
        // customer id
        channelBuffer.writeByte(U8.t((short) 0xff));
        //reserveds
        channelBuffer.writeByte(U8.t((short) 0));
        //Noviflow type : NOVI_MSG_UDP_PAYLOAD
        channelBuffer.writeByte(U8.t((short) tableId));
        //Number of bytes to match on : 3 for the vni
        channelBuffer.writeByte(U8.t((short) 3));
        //Payload offset : 4
        channelBuffer.writeShort(U16.t(4));

    }

    @Override
    public OFVersion getVersion() {
        // Do nothing here for now.
        return OFVersion.OF_13;
    }

    @Override
    public OFType getType() {
        // Do nothing here for now.
        return OFType.EXPERIMENTER;
    }

    private int bytesToInt(byte[] bytes, int startIndex) {
        return bytes[startIndex] << 24 | (bytes[startIndex + 1] & 0xff) << 16 | (bytes[startIndex + 2] & 0xff) << 8
                | (bytes[startIndex + 3] & 0xff);
    }

    private void addIntToBytes(byte[] bytes, int startIndex, int toAdd) {

        for (int i = 0; i < 4; i++) {
            bytes[startIndex + i] = (byte) ((toAdd  >> ((3 - i) * 8)) & 0xff);
        }

    }

    public void send(OpenFlowController ofCtrl, DeviceId deviceId) {
        ofCtrl.write(Dpid.dpid(deviceId.uri()), this);
    }

    public byte[] msgBytes() {

        byte[] msg = new byte[10];

        //Experimenter
        addIntToBytes(msg, 0, 0xff000002);
        // customer id
        msg[4] = (byte) 255;
        //reserved
        msg[5] = (byte) 0;
        //Noviflow type : NOVI_MSG_UDP_PAYLOAD
        msg[6] = (byte) tableId;
        //Number of bytes to match on : 3 for the vni
        msg[7] = (byte) 3;
        //Payload offset : 4
        msg[8] = 0;
        msg[9] = 4;


        return msg;
    }

    public byte[] ofHeader() {
        byte[] header = new byte[8];
        header[0] = (byte) 4;
        header[2] = (byte) 4;
        addIntToBytes(header, 4, 123456);

        return header;
    }

    public byte[] experimenterHeader() {

        byte[] ofHeader = ofHeader();
        byte[] header = new byte[ofHeader.length + 8];

        addIntToBytes(header, ofHeader.length, 0xff000002);
        addIntToBytes(header, ofHeader.length + 4, 0);

        return header;

    }

    public byte[] fullIPPayload() {

        byte[] experimenterHeader = experimenterHeader();
        byte[] msg = msgBytes();

        int size = experimenterHeader.length + msg.length;

        byte[] ipPayload = new byte[size];

        for (int i = 0; i < experimenterHeader.length; i++) {
            ipPayload[i] = experimenterHeader[i];
        }

        for (int i = 0; i < msg.length; i++) {
            ipPayload[i + experimenterHeader.length] = msg[i];
        }

        //set size
        ipPayload[3] = (byte) size;

        return ipPayload;
    }

}
