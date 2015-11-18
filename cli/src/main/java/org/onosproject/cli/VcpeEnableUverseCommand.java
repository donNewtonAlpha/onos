package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.apps.VcpeService;

/**
 * Created by nick on 7/13/15.
 */
@Command(scope = "onos-app-vcpe", name = "enableuverse",
        description = "create a flow connecting the client to Uverse")
public class VcpeEnableUverseCommand extends AbstractShellCommand {


    private VcpeService service;

    @Argument(index = 0, name = "clientId", description = "client identification number",
            required = true, multiValued = false)
    private int clientId;


    @Override
    protected void execute() {
        service = get(VcpeService.class);
        try {
            if (clientId < 3) {
                print("client is not real");
                return;
            }
            service.enableUverse(clientId);
        } catch (Exception e) {
            print("exception occured", e);
        }

    }
}
