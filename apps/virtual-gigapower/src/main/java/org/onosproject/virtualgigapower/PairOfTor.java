package org.onosproject.virtualgigapower;

import org.onosproject.net.PortNumber;

/**
 * Created by nick on 11/25/15.
 */
public class PairOfTor {

    private int pairMplsLabel;
    private Leaf leaf1;
    private Leaf leaf2;

    public PairOfTor(Leaf leaf1, Leaf leaf2,  int mplsLabel){
        this.leaf1 = leaf1;
        this.leaf2 = leaf2;
        this.pairMplsLabel = mplsLabel;
    }

    public PairOfTor(Leaf leaf1, Leaf leaf2){
        new PairOfTor(leaf1, leaf2, 0);
    }

    public int getPairMplsLabel() {
        return pairMplsLabel;
    }

    public void setPairMplsLabel(int pairMplsLabel) {
        this.pairMplsLabel = pairMplsLabel;
    }

    public void addBehavior(SwitchingBehavior beahavior){
        leaf1.addBehavior(beahavior);
        leaf2.addBehavior(beahavior);
    }

    public void connectToSpine(int portNumber){
        leaf1.connectToSpine(portNumber);
        leaf2.connectToSpine(portNumber);
    }

    public void disconnectFromSpine(int portNumber){
        leaf1.disconnectFromSpine(portNumber);
        leaf2.disconnectFromSpine(portNumber);
    }

    public void addInternetLink(PortNumber port){
        leaf1.addInternetLink(port);
        leaf2.addInternetLink(port);
    }

    public void removeInternetLink(PortNumber port){
        leaf1.removeInternetLink(port);
        leaf2.removeInternetLink(port);
    }
}
