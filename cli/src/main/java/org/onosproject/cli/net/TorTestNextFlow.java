package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.apps.TorService;

/**
 * Created by nick on 10/8/15.
 */
@Command(scope = "onos-app-tor", name = "tornextflow",
        description = "test the next flow")
public class TorTestNextFlow extends AbstractShellCommand {

        private TorService service;

        @Override
        protected void execute() {
            service = get(TorService.class);
            try {
                service.nextFlow();
                print("Next flow enabled, previous disabled");
            } catch (Exception e) {
                print("exception occured", e);
            }

        }


}
