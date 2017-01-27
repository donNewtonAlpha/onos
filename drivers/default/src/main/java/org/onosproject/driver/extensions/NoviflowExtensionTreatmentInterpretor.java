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


import org.onosproject.driver.extensions.ofmessages.OFActionNoviflowDecapsulateVxLan;
import org.onosproject.driver.extensions.ofmessages.OFActionNoviflowExperimenter;
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
        if (extensionTreatmentType.equals(
                ExtensionTreatmentType.ExtensionTreatmentTypes.NOVIFLOW_POP_VXLAN.type())) {
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

        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NOVIFLOW_POP_VXLAN.type())) {

            OFActionNoviflowDecapsulateVxLan action = new OFActionNoviflowDecapsulateVxLan();

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
                if (experimenterAction instanceof OFActionNoviflowExperimenter) {
                    if (((OFActionNoviflowExperimenter) experimenterAction).isSetVxLan()) {
                        return new NoviflowSetVxLan();
                    }
                }
                return new NoviflowPopVxLan();

            }


        }
        throw new UnsupportedOperationException(
                "Unexpected OFAction: " + action.toString());
    }

    @Override
    public ExtensionTreatment getExtensionInstruction(ExtensionTreatmentType type) {
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NOVIFLOW_SET_VXLAN.type())) {
            return new NoviflowSetVxLan();
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NOVIFLOW_POP_VXLAN.type())) {
            return new NoviflowPopVxLan();
        }
        throw new UnsupportedOperationException(
                "Driver does not support extension type " + type.toString());
    }
}
