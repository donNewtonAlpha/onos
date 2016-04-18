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
