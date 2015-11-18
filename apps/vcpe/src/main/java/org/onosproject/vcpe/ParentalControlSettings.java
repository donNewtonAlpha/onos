package org.onosproject.vcpe;

import org.onlab.packet.MacAddress;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

/**
 * Created by nick on 7/10/15.
 */
public class ParentalControlSettings {

    private boolean allowByDefault = true;
    private List<DeviceParentalControlSettings> devices = new LinkedList<>();

    public ParentalControlSettings(){
    }

    public void setDefault(boolean newDefaultSetting){
        this.allowByDefault = newDefaultSetting;
    }

    public void addDevice(MacAddress mac,  List<String> restrictions){
        devices.add( new DeviceParentalControlSettings(mac, restrictions));
    }
    public void addDevice(MacAddress mac){
        devices.add(new DeviceParentalControlSettings(mac));
    }

    public void removeDevice(MacAddress mac){
        Iterator<DeviceParentalControlSettings> it = devices.iterator();
        while(it.hasNext()){
            if(it.next().macMatch(mac)){
                it.remove();
            }
        }
    }

    public boolean isAllowed(MacAddress mac, Vector<String> categories){
        for(DeviceParentalControlSettings device : devices){
            if(device.macMatch(mac)){
                boolean b = true;
                for(String category : categories){
                    b = b&&device.isCategoryAllowed(category);
                }
                return b;
            }
        }
        return allowByDefault;
    }
    public boolean isDeviceRestricted(MacAddress mac){
        for(DeviceParentalControlSettings device : devices){
            if(device.macMatch(mac)){
                return device.isRestricted();
            }
        }
        return allowByDefault;
    }

    private class DeviceParentalControlSettings {
        private MacAddress mac;
        private List<String> restrictedCategories = new LinkedList<>();

        public DeviceParentalControlSettings(MacAddress newMachineMac, List<String> restrictions){
            mac = newMachineMac;
            restrictedCategories = restrictions;
        }

        DeviceParentalControlSettings(MacAddress newMachineMac){
            mac = newMachineMac;
        }
        boolean macMatch(MacAddress macToCompareTo){
            return mac.equals(macToCompareTo);
        }
        boolean isCategoryAllowed(String category){
            return !restrictedCategories.contains(category);
        }
        boolean isRestricted(){
            return !restrictedCategories.isEmpty();
        }
    }
}
