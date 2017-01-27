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

package org.onosproject.driver.extensions;

import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;


/**
 * Created by nick on 4/11/16.
 */
public class NoviflowPopVxLan extends AbstractExtension implements ExtensionTreatment {

     /**
     * Constructs a new set VxLAN instruction.
     */
    public NoviflowPopVxLan() {
    }


    @Override
    public ExtensionTreatmentType type() {
        return ExtensionTreatmentType.ExtensionTreatmentTypes.NOVIFLOW_POP_VXLAN.type();
    }

    @Override
    public void deserialize(byte[] data) {

    }


    @Override
    public byte[] serialize() {

        return new byte[2];
    }

    @Override
    public int hashCode() {

        return 12;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NoviflowPopVxLan) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "NoviFlow Pop VxLan";
    }
}
