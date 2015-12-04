package org.onosproject.virtualgigapower;

import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.LinkKey;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.*;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by nick on 12/4/15.
 */
public class MplsGroups {


    private static final MacAddress MPLS_MAC_DST = MacAddress.valueOf("aa:aa:aa:aa:aa:aa");
    private static final MacAddress MPLS_MAC_SRC = MacAddress.valueOf("bb:bb:bb:bb:bb:bb");
    private static final VlanId MPLS_VLAN = VlanId.vlanId((short)2015);

    private List<MplsInterfaceGroupInfo> mplsInterfaces;
    private List<MplsL2VpnGroupInfo> mplsL2VpnGroups;
    private DeviceId deviceId;


    public MplsGroups(DeviceId deviceId){
        this.deviceId = deviceId;
        this.mplsInterfaces = new LinkedList<>();
        this.mplsL2VpnGroups = new LinkedList<>();
    }


    private MplsL2VpnGroupInfo getMplsL2Vpn(PortNumber port, int mplsLabel){

        for(MplsL2VpnGroupInfo info : mplsL2VpnGroups){
            if(port.equals(info.getPortNumber())){
                if (mplsLabel==info.getMplsLabel()){
                    return info;
                }
            }

        }
        //This group is not created yet
        //Create and store

        //Get the Mpls interface
        MplsInterfaceGroupInfo mplsInterface = this.getMplsInterface(port);

        MplsL2VpnGroupInfo mplsL2VpnInfo = new MplsL2VpnGroupInfo(mplsLabel, mplsInterface);
        mplsL2VpnGroups.add(mplsL2VpnInfo);

        return mplsL2VpnInfo;

    }

    public GroupId getMplsL2VpnGroupId(PortNumber port, int mplsLabel){
        return this.getMplsL2Vpn(port, mplsLabel).getGroupId();
    }

    private MplsInterfaceGroupInfo getMplsInterface(PortNumber port){

        for(MplsInterfaceGroupInfo info: mplsInterfaces){

            if(port.equals(info.getPortNumber())){
                return info;
            }

        }
        //This group is not created yet
        //Create and store
        MplsInterfaceGroupInfo newGroupInfo = new MplsInterfaceGroupInfo(port);
        mplsInterfaces.add(newGroupInfo);

        return newGroupInfo;

    }
    public GroupId getMplsInterfaceGroupId(PortNumber port){

        return this.getMplsInterface(port).getGroupId();

    }




    public class MplsInterfaceGroupInfo {

        private PortNumber port;
        private GroupId groupId;


        public MplsInterfaceGroupInfo(PortNumber port){
            this.port = port;

            //L2 Unfiltered Interface

            TrafficTreatment.Builder l2Output = DefaultTrafficTreatment.builder();
            l2Output.setOutput(port);

            Integer l2UnfilteredGroupIdentifier =  ((11 <<  28) |((short) port.toLong()));
            final GroupKey l2UnfilteredGroupkey = new DefaultGroupKey(ByteBuffer.allocate(4).putInt(l2UnfilteredGroupIdentifier).array());

            GroupBucket l2UnfilteredBucket =
                    DefaultGroupBucket.createIndirectGroupBucket(l2Output.build());
            GroupDescription l2UnfilteredGroupDescription = new DefaultGroupDescription(deviceId,
                    GroupDescription.Type.INDIRECT,
                    new GroupBuckets(Collections.singletonList(l2UnfilteredBucket)),
                    l2UnfilteredGroupkey,
                    l2UnfilteredGroupIdentifier,
                    VirtualGigaPowerComponent.appId);

            VirtualGigaPowerComponent.groupService.addGroup(l2UnfilteredGroupDescription);


            // Mpls interface group

            TrafficTreatment.Builder mplsInterfaceTreatment = DefaultTrafficTreatment.builder();
            mplsInterfaceTreatment.setEthDst(MPLS_MAC_DST);
            mplsInterfaceTreatment.setEthSrc(MPLS_MAC_SRC);
            mplsInterfaceTreatment.setVlanId(MPLS_VLAN);
            mplsInterfaceTreatment.group(VirtualGigaPowerComponent.groupService.getGroup(deviceId, l2UnfilteredGroupkey).id());

            Integer mplsInterfaceGroupIdentifier = (9 << 28 | ((short) port.toLong()));
            final GroupKey mplsInterfaceGroupKey = new DefaultGroupKey(ByteBuffer.allocate(4).putInt(mplsInterfaceGroupIdentifier).array());

            GroupBucket mplsInterfaceBucket =
                    DefaultGroupBucket.createIndirectGroupBucket(mplsInterfaceTreatment.build());
            GroupDescription mplsInterfaceGroupDescription = new DefaultGroupDescription(deviceId,
                    GroupDescription.Type.INDIRECT,
                    new GroupBuckets((Collections.singletonList(mplsInterfaceBucket))),
                    mplsInterfaceGroupKey,
                    mplsInterfaceGroupIdentifier,
                    VirtualGigaPowerComponent.appId);

            VirtualGigaPowerComponent.groupService.addGroup(mplsInterfaceGroupDescription);

            this.groupId = VirtualGigaPowerComponent.groupService.getGroup(deviceId, mplsInterfaceGroupKey).id();


        }

        public PortNumber getPortNumber(){
            return port;
        }
        public GroupId getGroupId(){
            return  groupId;
        }

    }



    public class MplsL2VpnGroupInfo {

        private int mplsLabel;
        private PortNumber port;
        private MplsInterfaceGroupInfo parentMplsInterface;
        private GroupId groupId;


        public MplsL2VpnGroupInfo(int mplsLabel, MplsInterfaceGroupInfo mplsInterface){

            this.mplsLabel = mplsLabel;
            this.port = mplsInterface.getPortNumber();
            this.parentMplsInterface = mplsInterface;


            TrafficTreatment.Builder mplsL2VpnTreatment = DefaultTrafficTreatment.builder();
            //TODO : extend ONOS
            //mplsL2VpnTreatment.pushL2Header();
            mplsL2VpnTreatment.pushMpls();
            mplsL2VpnTreatment.setMpls(MplsLabel.mplsLabel(mplsLabel));
            mplsL2VpnTreatment.group(mplsInterface.getGroupId());

            Integer mplsL2VpnGroupIdentifier = (9 << 28 | 1 << 24 | ((short) (port.toLong()*10000 + mplsLabel)));
            final GroupKey mplsL2VpnGroupKey = new DefaultGroupKey(ByteBuffer.allocate(4).putInt(mplsL2VpnGroupIdentifier).array());

            GroupBucket mplsL2VpnBucket =
                    DefaultGroupBucket.createIndirectGroupBucket(mplsL2VpnTreatment.build());
            GroupDescription mplsL2VpnGroupDescription = new DefaultGroupDescription(deviceId,
                    GroupDescription.Type.INDIRECT,
                    new GroupBuckets((Collections.singletonList(mplsL2VpnBucket))),
                    mplsL2VpnGroupKey,
                    mplsL2VpnGroupIdentifier,
                    VirtualGigaPowerComponent.appId);

            VirtualGigaPowerComponent.groupService.addGroup(mplsL2VpnGroupDescription);

            this.groupId = VirtualGigaPowerComponent.groupService.getGroup(deviceId, mplsL2VpnGroupKey).id();

        }

        public PortNumber getPortNumber(){
            return port;
        }

        public GroupId getGroupId(){
            return groupId;
        }

        public int getMplsLabel(){
            return mplsLabel;
        }

    }


}
