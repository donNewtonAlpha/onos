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

package org.onosproject.noviaggswitch;

import org.omg.CORBA.Object;
import org.onlab.packet.Ip4Address;
import org.onosproject.net.DeviceId;

/**
 * Created by nick on 9/20/16.
 */
public class VxlanTunnelId {

    private DeviceId deviceId;
    private Ip4Address ip;
    private int vni;

    public VxlanTunnelId(DeviceId deviceId, Ip4Address ip, int vni) {
        this.deviceId = deviceId;
        this.ip = ip;
        this.vni = vni;
    }

    public boolean equals(Object object) {

        if (object instanceof VxlanTunnelId) {
            VxlanTunnelId other = (VxlanTunnelId) object;
            if (other.deviceId.equals(deviceId) && other.ip.equals(ip) && other.vni == vni) {
                return true;
            }
        }
        return false;
    }

}
