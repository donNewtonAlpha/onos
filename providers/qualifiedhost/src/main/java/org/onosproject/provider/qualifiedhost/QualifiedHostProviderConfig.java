package org.onosproject.provider.qualifiedhost;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by nick on 3/15/16.
 */
public class QualifiedHostProviderConfig extends Config<ApplicationId> {

    private static Logger log = LoggerFactory.getLogger(QualifiedHostProviderConfig.class);
    public static final String SWITCH_CONFIGS = "switchConfigs";

    private static final String DEVICE_ID = "deviceId";
    private static final String VLANS = "vlans";

    @Override
    public boolean isValid() {
        return hasOnlyFields(SWITCH_CONFIGS);
    }

    public List<SwitchConfig> switchConfigs() {

        List<SwitchConfig> switchConfigs = new LinkedList<>();

        ArrayNode jsonArray = (ArrayNode) object.path(SWITCH_CONFIGS);
        for (JsonNode node : jsonArray) {
            try {
                //Each node contains the "deviceId" (JsonString), and an array of (JsonInt) "vlans"

                String newDeviceId = node.path(DEVICE_ID).asText("");
                DeviceId deviceId = DeviceId.deviceId(newDeviceId);

                ArrayNode vlansArray = (ArrayNode) node.path(VLANS);
                List<VlanId> vlanIds = new LinkedList<>();
                for (JsonNode vlanNode : vlansArray) {
                    int vlan = vlanNode.asInt(-2);
                    if (vlan == -1) {
                        vlanIds.add(VlanId.NONE);
                    } else if (vlan > 4096 || vlan < 0) {
                        log.warn("Invalid Vlan provided");
                    } else {
                        vlanIds.add(VlanId.vlanId((short) vlan));
                    }
                }
                switchConfigs.add(new SwitchConfig(deviceId, vlanIds));


            } catch (Exception e) {
                log.error("Parsing exception", e);
            }
        }

        return switchConfigs;
    }

    public class SwitchConfig {

        private DeviceId deviceId;
        private List<VlanId> vlans;

        public SwitchConfig(DeviceId deviceId, List<VlanId> vlans) {
            this.deviceId = deviceId;
            this.vlans = vlans;
        }

        public DeviceId getDeviceId() {
            return deviceId;
        }

        public List<VlanId> getVlans() {
            return vlans;
        }


    }


}
