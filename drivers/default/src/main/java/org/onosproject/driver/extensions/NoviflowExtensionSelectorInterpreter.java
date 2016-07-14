package org.onosproject.driver.extensions;

import org.onlab.packet.VlanId;
import org.onosproject.net.behaviour.ExtensionSelectorResolver;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;
import org.onosproject.openflow.controller.ExtensionSelectorInterpreter;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmVlanVid;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmVlanVidMasked;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.VlanVid;

/**
 * Created by nick on 7/14/16.
 */
public class NoviflowExtensionSelectorInterpreter extends AbstractHandlerBehaviour
        implements ExtensionSelectorInterpreter, ExtensionSelectorResolver {

    @Override
    public boolean supported(ExtensionSelectorType extensionSelectorType) {
        if (extensionSelectorType.equals(ExtensionSelectorType.ExtensionSelectorTypes.OFDPA_MATCH_VLAN_VID.type())) {
            return true;
        }
        return false;
    }

    @Override
    public OFOxm<?> mapSelector(OFFactory factory, ExtensionSelector extensionSelector) {
        ExtensionSelectorType type = extensionSelector.type();
        if (type.equals(ExtensionSelectorType.ExtensionSelectorTypes.OFDPA_MATCH_VLAN_VID.type())) {
            VlanId vlanId = ((OfdpaMatchVlanVid) extensionSelector).vlanId();
            // Special VLAN 0x0000/0x1FFF required by OFDPA
            if (vlanId.equals(VlanId.NONE)) {
                OFVlanVidMatch vid = OFVlanVidMatch.ofRawVid((short) 0x0000);
                OFVlanVidMatch mask = OFVlanVidMatch.ofRawVid((short) 0x1FFF);
                return factory.oxms().vlanVidMasked(vid, mask);
                // Normal case
            } else if (vlanId.equals(VlanId.ANY)) {
                return factory.oxms().vlanVidMasked(OFVlanVidMatch.PRESENT, OFVlanVidMatch.PRESENT);
            } else {
                return factory.oxms().vlanVid(OFVlanVidMatch.ofVlanVid(VlanVid.ofVlan(vlanId.toShort())));
            }
        }
        throw new UnsupportedOperationException(
                "Unexpected ExtensionSelector: " + extensionSelector.toString());
    }

    @Override
    public ExtensionSelector mapOxm(OFOxm<?> oxm) {
        VlanId vlanId;

        if (oxm.getMatchField().equals(MatchField.VLAN_VID)) {
            if (oxm.isMasked()) {
                OFVlanVidMatch vid = ((OFOxmVlanVidMasked) oxm).getValue();
                OFVlanVidMatch mask = ((OFOxmVlanVidMasked) oxm).getMask();

                if (vid.equals(OFVlanVidMatch.ofRawVid((short) 0))) {
                    vlanId = VlanId.NONE;
                } else if (vid.equals(OFVlanVidMatch.PRESENT) &&
                        mask.equals(OFVlanVidMatch.PRESENT)) {
                    vlanId = VlanId.ANY;
                } else {
                    vlanId = VlanId.vlanId(vid.getVlan());
                }
            } else {
                OFVlanVidMatch vid = ((OFOxmVlanVid) oxm).getValue();

                if (!vid.isPresentBitSet()) {
                    vlanId = VlanId.NONE;
                } else {
                    vlanId = VlanId.vlanId(vid.getVlan());
                }
            }
            return new OfdpaMatchVlanVid(vlanId);
        }
        throw new UnsupportedOperationException(
                "Unexpected OXM: " + oxm.toString());
    }

    @Override
    public ExtensionSelector getExtensionSelector(ExtensionSelectorType type) {
        if (type.equals(ExtensionSelectorType.ExtensionSelectorTypes.OFDPA_MATCH_VLAN_VID.type())) {
            return new OfdpaMatchVlanVid();
        }
        throw new UnsupportedOperationException(
                "Driver does not support extension type " + type.toString());
    }
}
