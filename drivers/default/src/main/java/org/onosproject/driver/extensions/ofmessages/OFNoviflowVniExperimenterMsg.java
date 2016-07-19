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
public class OFNoviflowVniExperimenterMsg extends ThirdPartyMessage{

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
        channelBuffer.writeByte(U8.t((short)0xff));
        //reserveds
        channelBuffer.writeByte(U8.t((short)0));
        //Noviflow type : NOVI_MSG_UDP_PAYLOAD
        channelBuffer.writeByte(U8.t((short)tableId));
        //Number of bytes to match on : 3 for the vni
        channelBuffer.writeByte(U8.t((short)3));
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

    public void send(OpenFlowController ofCtrl, DeviceId deviceId) {
        ofCtrl.write(Dpid.dpid(deviceId.uri()), this);
    }

}
