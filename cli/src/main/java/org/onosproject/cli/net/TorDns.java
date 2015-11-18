package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.apps.TorService;

/**
 * Created by nick on 10/8/15.
 */
@Command(scope = "onos-app-tor", name = "torDns",
        description = "enable/disable dns service for  this vlanId")
public class TorDns extends AbstractShellCommand {

    private TorService service;

    @Argument(index = 0, name = "action", description = "enable/disable",
            required = true, multiValued = false)
    private String action;

    @Argument(index = 1, name = "clientId", description = "client vlanId",
            required = true, multiValued = false)
    private int clientId;


    @Override
    protected void execute() {
        service = get(TorService.class);
        try {
            switch (action) {
                case "enable":
                    service.dnsService(clientId);
                    print("Dns service enabled for this customer");
                    break;
                case "disable":
                    service.noDnsService(clientId);
                    print("Dns service disabled for this customer");
                    break;
                default:
                    print("Unknown action");
            }
        } catch (Exception e) {
            print("exception occured", e);
        }

    }


}