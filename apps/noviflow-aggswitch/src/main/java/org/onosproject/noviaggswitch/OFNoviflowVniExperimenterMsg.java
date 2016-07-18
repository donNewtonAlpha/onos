package org.onosproject.noviaggswitch;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.openflow.controller.ThirdPartyMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.types.U16;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.types.U8;

/**
 * Created by nick on 7/18/16.
 */
public class OFNoviflowVniExperimenterMsg extends ThirdPartyMessage{

    int tableId;

    public OFNoviflowVniExperimenterMsg(int tableId){
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
        //reserved
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
        return OFVersion.OF_13;
    }

    @Override
    public OFType getType() {
        return OFType.EXPERIMENTER;
    }

}
