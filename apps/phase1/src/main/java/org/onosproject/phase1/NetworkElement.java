package org.onosproject.phase1;

import org.onosproject.net.PortNumber;

/**
 * Created by nick on 2/11/16.
 */
public abstract class NetworkElement {

    private PortNumber portNumber;

    public PortNumber getPortNumber() {
        return portNumber;
    }

    protected void setPortNumber(PortNumber portNumber) {
        this.portNumber = portNumber;
    }
}
