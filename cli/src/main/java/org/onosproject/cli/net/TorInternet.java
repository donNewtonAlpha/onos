package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.apps.TorService;

/**
 * Created by nick on 10/8/15.
 */
@Command(scope = "onos-app-tor", name = "torInternet",
        description = "enable/disable internet for  this vlanId")
public class TorInternet extends AbstractShellCommand {

        private TorService service;

        @Argument(index = 0, name = "action", description = "enable/disable",
            required = true, multiValued = false)
        private String action;

        @Argument(index = 1, name = "clientId", description = "client identification number",
                required = true, multiValued = false)
        private int clientId;


        @Override
        protected void execute() {
            service = get(TorService.class);
            try {
                Runnable r = null;
                switch (action) {
                    case "enable":
                        r = new Runnable() {
                            @Override
                            public void run() {
                                service.connectInternet(clientId);
                                print("customer connected to the internet");
                            }
                        };


                        break;
                    case "disable":
                        service.disconnectInternet(clientId);
                        print("customer disconnected from the Internet");
                        break;
                    default:
                        print("Unknown action");
                }

                if (r != null) {
                    Thread thread = new Thread(r);
                    thread.setDaemon(true);
                    thread.start();
                }
            } catch (Exception e) {
                print("exception occured", e);
            }

        }


}
