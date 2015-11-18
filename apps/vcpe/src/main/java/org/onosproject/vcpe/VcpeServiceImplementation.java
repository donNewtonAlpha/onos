package org.onosproject.vcpe;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.apps.VcpeService;

/**
 * Created by nick on 6/29/15.
 */
@Component(immediate = true)
@Service
public class VcpeServiceImplementation implements VcpeService{


    private static InternetSwitch internetSwitch;
    private static LanSwitch lanSwitch;

    public static  void setSwitches(InternetSwitch internet, LanSwitch lan){
        internetSwitch = internet;
        lanSwitch = lan;
    }

    @Override
    public void enableInternet(int clientId) {
        internetSwitch.connectCustomer(clientId);
    }
    @Override
    public void disableInternet(int clientId) {
        internetSwitch.disconnectCustomer(clientId);
    }
    @Override
    public void enableUverse(int clientId) {
        lanSwitch.connectCustomer(clientId);
    }
    @Override
    public void disableUverse(int clientId) {
        lanSwitch.disconnectCustomer(clientId);
    }
    @Override
    public void generalDNS(int clientId) {
        VcpeComponent.log.info("vcpe service implementation generalDns call initiated");
        lanSwitch.generalDns(clientId);
        VcpeComponent.log.info("vcpe service implementation generalDns call completed");
    }
    @Override
    public void personalDNS(int clientId) {
        lanSwitch.personalDns(clientId);
    }
    @Override
    public void noDNS(int clientId) {
        lanSwitch.noDns(clientId);
    }
    @Override
    public void initiateCustomer(int clientId) {
        try {
            lanSwitch.initiateCustomer(clientId);
        }catch(Exception e){
            VcpeComponent.log.error("exception occured, initiateCustomer", e);
        }
    }

    public void discoverLan(int clientId){
        try{
            lanSwitch.discoverLan(clientId);
        }catch(Exception e){
            VcpeComponent.log.error("Lan discovery exception", e);
        }
    }

}
