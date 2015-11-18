package org.onosproject.vcpe;

import org.onlab.packet.MacAddress;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by nick on 7/16/15.
 */
public class TestParentalControl {

    public static void fakeEntry(LanSwitch lanSwitch, int clientId){

        ParentalControlSettings clientSettings = lanSwitch.getParentalControlSettings().get(clientId);
        MacAddress mac1 = MacAddress.valueOf("6c:88:14:54:25:d4");
        List<String> restrictions1 = new LinkedList<>();
        restrictions1.add("politics");
        restrictions1.add("religion");
        restrictions1.add("news");
        restrictions1.add("gamble");
        clientSettings.addDevice(mac1, restrictions1);

        MacAddress mac2 = MacAddress.valueOf("88:c9:d0:c0:0f:87");
        List<String> restrictions2 = new LinkedList<>();
        restrictions2.add("gamble");
        clientSettings.addDevice(mac2, restrictions2);


    }
}
