package org.onosproject.virtualgigapower;

import org.onosproject.net.PortNumber;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by nick on 11/25/15.
 */
public class PairOfTor {

    private int pairMplsLabel;
    private Leaf leaf1;
    private Leaf leaf2;
    private List<SwitchingBehavior> groupBehaviors;
    private List<PortNumber> spineConnections;

    public PairOfTor(Leaf leaf1, Leaf leaf2,  int mplsLabel){
        this.leaf1 = leaf1;
        this.leaf2 = leaf2;
        this.pairMplsLabel = mplsLabel;
        this.groupBehaviors = new LinkedList<>();
        this.spineConnections = new LinkedList<>();
    }

    public PairOfTor(Leaf leaf1, Leaf leaf2){
        new PairOfTor(leaf1, leaf2, 0);
    }

    public List<Leaf> getLeaves(){
        List<Leaf> leaves = new LinkedList<>();
        leaves.add(leaf1);
        leaves.add(leaf2);
        return leaves;
    }

    public int getPairMplsLabel() {
        return pairMplsLabel;
    }

    public List<SwitchingBehavior> getBehaviors(){
        return groupBehaviors;
    }

    public void setPairMplsLabel(int pairMplsLabel) {
        this.pairMplsLabel = pairMplsLabel;
    }

    public void addBehavior(SwitchingBehavior beahavior){
        groupBehaviors.add(beahavior);
    }

    public void connectToSpine(int portNumber){
        spineConnections.add(PortNumber.portNumber(portNumber));
    }

    public void disconnectFromSpine(int portNumber){
        for (PortNumber port: spineConnections) {
            if(port.toLong() == portNumber){
                spineConnections.remove(port);
            }
        }
    }

    public List<PortNumber> getSpineConnections(){
        return spineConnections;
    }

    public void addInternetLink(PortNumber port){
        leaf1.addInternetLink(port);
        leaf2.addInternetLink(port);
    }

    public void removeInternetLink(PortNumber port){
        leaf1.removeInternetLink(port);
        leaf2.removeInternetLink(port);
    }

    public  boolean containsOlt(int sTag){

        for(SwitchingBehavior behavior: groupBehaviors){
            if(behavior instanceof OltBehavior){
                if(((OltBehavior)behavior).getSTag() == sTag){
                    return true;
                }
            }
        }
        return false;
    }

    public  boolean containsServer(int sTag){

        for(SwitchingBehavior behavior: groupBehaviors){
            if(behavior instanceof CustomerServerBehavior){
                if(((CustomerServerBehavior)behavior).getSTag() == sTag){
                    return true;
                }
            }
        }
        return false;
    }

    public OltBehavior getOlt(int sTag){

        for(SwitchingBehavior behavior: groupBehaviors){
            if(behavior instanceof OltBehavior){
                if(((OltBehavior)behavior).getSTag() == sTag){
                    return (OltBehavior)behavior;
                }
            }
        }
        return null;
    }

    public CustomerServerBehavior getServer(int sTag){

        for(SwitchingBehavior behavior: groupBehaviors){
            if(behavior instanceof CustomerServerBehavior){
                if(((CustomerServerBehavior)behavior).getSTag() == sTag){
                    return (CustomerServerBehavior)behavior;
                }
            }
        }
        return null;
    }
}
