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
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFActionExperimenter;
import org.projectfloodlight.openflow.types.U16;
import org.projectfloodlight.openflow.types.U32;


/**
 * Created by nick on 4/5/16.
 */
public class OFActionNoviflowVxLan implements OFActionExperimenter {


    private MacAddress dstMac;
    private MacAddress srcMac;
    private Ip4Address dstIp;
    private Ip4Address srcIp;
    private int udpPort;
    private int vni;

    public OFActionNoviflowVxLan(MacAddress dstMac, MacAddress srcMac, Ip4Address dstIp,
                                 Ip4Address srcIp, int udpPort, int vni) {
        this.srcMac = srcMac;
        this.dstMac = dstMac;
        this.dstIp = dstIp;
        this.srcIp = srcIp;
        this.udpPort = udpPort;
        this.vni = vni;
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
        channelBuffer.writeShort(U16.t(40));
        //Noviflow experimenter ID
        channelBuffer.writeInt(U32.t(0xff000002));
        //Customer Id
        channelBuffer.writeByte(255);
        //reserved field (0)
        channelBuffer.writeByte(0);
        //noviflow action type
        channelBuffer.writeShort(2);
        //Noviflow tunnel type: vxlan
        channelBuffer.writeByte(0);
        //flags
        channelBuffer.writeByte(1);
        //eht_src
        channelBuffer.writeBytes(srcMac.toBytes());
        //eth_dst
        channelBuffer.writeBytes(dstMac.toBytes());
        //Ip_src
        channelBuffer.writeInt(srcIp.toInt());
        //Ip_dst
        channelBuffer.writeInt(dstIp.toInt());
        //udp port
        channelBuffer.writeShort(U16.t(udpPort));
        //vni
        channelBuffer.writeInt(U32.t(vni & 0xffffff));

    }

    @Override
    public Builder createBuilder() {
        return null;
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putShort((short) -1);
        sink.putShort((short) 40);
        sink.putInt(0xff000002);
        sink.putByte((byte) -1);
        sink.putByte((byte) 0);
        sink.putShort((short) 2);
        sink.putByte((byte) 0);
        sink.putByte((byte) 1);
        sink.putBytes(srcMac.toBytes());
        sink.putBytes(dstMac.toBytes());
        sink.putInt(srcIp.toInt());
        sink.putInt(dstIp.toInt());
        sink.putShort((short) udpPort);
        sink.putInt(vni);

    }

    public boolean isSetVxLan() {
        return true;
    }
}
