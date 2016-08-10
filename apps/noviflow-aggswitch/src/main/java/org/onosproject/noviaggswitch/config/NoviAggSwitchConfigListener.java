package org.onosproject.noviaggswitch.config;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.basics.SubjectFactories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by nick on 8/9/16.
 */
public class NoviAggSwitchConfigListener implements NetworkConfigListener {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void event(NetworkConfigEvent event){


        if(event.configClass().equals(NoviAggSwitchConfig.class)) {

            switch(event.type()){
                case CONFIG_ADDED:
                    log.info("NoviAggSwitch Config added");
                    break;
                case CONFIG_UPDATED:
                    log.info("NoviAggSwitch config updated");
                    break;
                case CONFIG_REMOVED:
                    log.info("NoviAggSwitch Config removed");
                    break;
                default:
                    log.info("NoviAggSwitch config, unexpected action : "  + event.type());
            }

        }

    }


    public ConfigFactory<ApplicationId, NoviAggSwitchConfig> getCfgAppFactory() {

        return new ConfigFactory<ApplicationId, NoviAggSwitchConfig>(SubjectFactories.APP_SUBJECT_FACTORY,
                NoviAggSwitchConfig.class,
                "phase1") {
            @Override
            public NoviAggSwitchConfig createConfig() {
                return new NoviAggSwitchConfig();
            }
        };
    }


}
