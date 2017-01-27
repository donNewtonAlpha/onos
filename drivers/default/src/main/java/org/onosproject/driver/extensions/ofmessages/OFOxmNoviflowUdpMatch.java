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
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;
import org.projectfloodlight.openflow.types.OFValueType;
import org.projectfloodlight.openflow.types.U16;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.types.U8;

/**
 * Created by nick on 7/18/16.
 */
public class OFOxmNoviflowUdpMatch implements OFOxm {

    //Used only to match vni for now
    int vni;

    public OFOxmNoviflowUdpMatch(int vni) {
        this.vni = vni;
    }

    public int getVni() {
        return vni;
    }

    @Override
    public long getTypeLen() {
        return 0;
    }

    @Override
    public OFValueType getValue() {
        return U32.NO_MASK;
    }

    @Override
    public OFValueType getMask() {
        return null;
    }

    @Override
    public MatchField getMatchField() {
        return MatchField.REG0;
    }

    @Override
    public boolean isMasked() {
        return false;
    }

    @Override
    public OFOxm getCanonical() {
        return null;
    }

    @Override
    public OFVersion getVersion() {
        return OFVersion.OF_13;
    }

    @Override
    public void writeTo(ChannelBuffer channelBuffer) {

        // toxm experimenter
        channelBuffer.writeShort(U16.t(0xffff));
        //NOVI_MATCH_UDP_PAYLOAD and no mask
        channelBuffer.writeByte(U8.t((short) 2));
        //length
        channelBuffer.writeByte(U8.t((short) 11));
        //Noviflow experimenter
        channelBuffer.writeInt(U32.t(0xff000002));
        //Vni
        for (int i = 0; i < 3; i++) {
            channelBuffer.writeByte(U8.t((short) ((vni >> ((2 - i) * 8)) & 0xff)));
        }



    }

    @Override
    public Builder createBuilder() {
        return null;
    }

    @Override
    public void putTo(PrimitiveSink primitiveSink) {

    }
}
