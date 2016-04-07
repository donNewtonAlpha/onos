package org.onosproject.driver.extensions;


import org.onosproject.driver.extensions.ofmessages.OFActionNoviflowVxLan;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.openflow.controller.ExtensionTreatmentInterpreter;
import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionExperimenter;


/**
 * Created by nick on 4/5/16.
 */
public class NoviflowExtensionTreatmentInterpretor extends AbstractHandlerBehaviour
        implements ExtensionTreatmentInterpreter, ExtensionTreatmentResolver {
    @Override

    public boolean supported(ExtensionTreatmentType extensionTreatmentType) {
        if (extensionTreatmentType.equals(
                ExtensionTreatmentType.ExtensionTreatmentTypes.NOVIFLOW_SET_VXLAN.type())) {
            return true;
        }
        return false;
    }

    @Override
    public OFAction mapInstruction(OFFactory factory, ExtensionTreatment extensionTreatment) {
        ExtensionTreatmentType type = extensionTreatment.type();
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NOVIFLOW_SET_VXLAN.type())) {

            NoviflowSetVxLan vxlanTreatment = ((NoviflowSetVxLan) extensionTreatment);

            OFActionNoviflowVxLan action = new OFActionNoviflowVxLan(vxlanTreatment.getDstMac(),
                    vxlanTreatment.getSrcMac(), vxlanTreatment.getDstIp(), vxlanTreatment.getSrcIp(),
                    vxlanTreatment.getUdpPort(), vxlanTreatment.getVxLanId());

            return action;
        }
        throw new UnsupportedOperationException(
                "Unexpected ExtensionTreatment: " + extensionTreatment.toString());
    }

    @Override
    public ExtensionTreatment mapAction(OFAction action) {

        if (action.getType().equals(OFActionType.EXPERIMENTER)) {
            OFActionExperimenter experimenterAction = (OFActionExperimenter) action;
            if (experimenterAction.getExperimenter() == 0xff000002) {
                return new NoviflowSetVxLan();
            }
            //TODO : improve and add pop vxlan tunnel



        }
        throw new UnsupportedOperationException(
                "Unexpected OFAction: " + action.toString());
    }

    @Override
    public ExtensionTreatment getExtensionInstruction(ExtensionTreatmentType type) {
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NOVIFLOW_SET_VXLAN.type())) {
            return new NoviflowSetVxLan();
        }
        throw new UnsupportedOperationException(
                "Driver does not support extension type " + type.toString());
    }
}
