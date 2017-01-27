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

package org.onosproject.driver.extensions.ofmessages;

import com.google.common.hash.PrimitiveSink;
import org.jboss.netty.buffer.ChannelBuffer;
import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFActionExperimenter;
import org.projectfloodlight.openflow.types.U16;
import org.projectfloodlight.openflow.types.U32;

/**
 * Created by nick on 4/11/16.
 */
public class OFActionNoviflowDecapsulateVxLan implements OFActionExperimenter, OFActionNoviflowExperimenter {


    public OFActionNoviflowDecapsulateVxLan() {
    }

    @Override
    public OFActionType getType() {
        return OFActionType.EXPERIMENTER;
    }

    @Override
    public long getExperimenter() {
        return 0xff000002;
    }

    @Override
    public OFVersion getVersion() {
        return OFVersion.OF_13;
    }

    @Override
    public void writeTo(ChannelBuffer channelBuffer) {
        // type experimenter
        channelBuffer.writeShort(U16.t(0xffff));
        //length
        channelBuffer.writeShort(U16.t(16));
        //Noviflow experimenter ID
        channelBuffer.writeInt(U32.t(0xff000002));
        //Customer Id
        channelBuffer.writeByte(255);
        //reserved field (0)
        channelBuffer.writeByte(0);
        //noviflow action type (popTunnel)
        channelBuffer.writeShort(3);
        //Noviflow tunnel type: vxlan
        channelBuffer.writeByte(0);
        //padding
        channelBuffer.writeBytes(new byte[] {0, 0, 0});


    }

    @Override
    public Builder createBuilder() {
        return null;
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putShort((short) -1);
        sink.putShort((short) 16);
        sink.putInt(0xff000002);
        sink.putByte((byte) -1);
        sink.putByte((byte) 0);
        sink.putShort((short) 3);
        sink.putByte((byte) 0);
        sink.putBytes(new byte[] {0, 0, 0});

    }

    public boolean isSetVxLan() {
        return false;
    }
}
