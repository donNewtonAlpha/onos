package org.onosproject.driver.extensions;


import org.onosproject.driver.extensions.ofmessages.OFOxmNoviflowUdpMatch;
import org.onosproject.net.behaviour.ExtensionSelectorResolver;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;
import org.onosproject.openflow.controller.ExtensionSelectorInterpreter;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;


/**
 * Created by nick on 7/14/16.
 */
public class NoviflowExtensionSelectorInterpreter extends AbstractHandlerBehaviour
        implements ExtensionSelectorInterpreter, ExtensionSelectorResolver {

    @Override
    public boolean supported(ExtensionSelectorType extensionSelectorType) {
        if (extensionSelectorType.equals(ExtensionSelectorType.ExtensionSelectorTypes.NOVIFLOW_MATCH_UDP_PAYLOAD.type())) {
            return true;
        }
        return false;
    }

    @Override
    public OFOxm<?> mapSelector(OFFactory factory, ExtensionSelector extensionSelector) {
        ExtensionSelectorType type = extensionSelector.type();
        if (type.equals(ExtensionSelectorType.ExtensionSelectorTypes.NOVIFLOW_MATCH_UDP_PAYLOAD.type())) {

            NoviflowMatchVni vniMatch = (NoviflowMatchVni) extensionSelector;

            return new OFOxmNoviflowUdpMatch(vniMatch.getVni());


        }
        throw new UnsupportedOperationException(
                "Unexpected ExtensionSelector: " + extensionSelector.toString());
    }

    @Override
    public ExtensionSelector mapOxm(OFOxm<?> oxm) {

        if (oxm instanceof OFOxmNoviflowUdpMatch) {


            return new NoviflowMatchVni(((OFOxmNoviflowUdpMatch) oxm).getVni());
        }
        throw new UnsupportedOperationException(
                "Unexpected OXM: " + oxm.toString());
    }

    @Override
    public ExtensionSelector getExtensionSelector(ExtensionSelectorType type) {
        if (type.equals(ExtensionSelectorType.ExtensionSelectorTypes.NOVIFLOW_MATCH_UDP_PAYLOAD.type())) {
            return new NoviflowMatchVni();
        }
        throw new UnsupportedOperationException(
                "Driver does not support extension type " + type.toString());
    }
}
