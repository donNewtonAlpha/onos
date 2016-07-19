package org.onosproject.driver.extensions;

import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;


/**
 * Created by nick on 7/18/16.
 */


public class NoviflowMatchVni extends AbstractExtension implements ExtensionSelector{

    int vni;

    public NoviflowMatchVni() {}

    public NoviflowMatchVni(int vni) {
        this.vni = vni;
    }

    public int getVni() {
        return vni;
    }

    @Override
    public byte[] serialize() {
        return new byte[0];
    }

    @Override
    public void deserialize(byte[] data) {

    }

    @Override
    public ExtensionSelectorType type() {
        return ExtensionSelectorType.ExtensionSelectorTypes.NOVIFLOW_MATCH_UDP_PAYLOAD.type();
    }
}
