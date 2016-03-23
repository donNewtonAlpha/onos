/*
 * Copyright 2014-2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.provider.qualifiedhost;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP6;
import org.onlab.packet.IPacket;
import org.onlab.packet.IPv6;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.packet.ipv6.IExtensionHeader;
import org.onlab.packet.ndp.NeighborAdvertisement;
import org.onlab.packet.ndp.NeighborSolicitation;
import org.onlab.packet.ndp.RouterAdvertisement;
import org.onlab.packet.ndp.RouterSolicitation;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.driver.extensions.OfdpaMatchVlanVid;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.Host;
import org.onosproject.net.Device;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.HostProviderRegistry;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which uses an OpenFlow controller to detect network end-station
 * hosts.
 */
@Component(immediate = true)
public class QualifiedHostLocationProvider extends AbstractProvider implements HostProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry netCfgService;

    private HostProviderService providerService;

    private final InternalHostProvider processor = new InternalHostProvider();
    private final DeviceListener deviceListener = new InternalDeviceListener();

    private ApplicationId appId;

    @Property(name = "hostRemovalEnabled", boolValue = true,
            label = "Enable host removal on port/device down events")
    private boolean hostRemovalEnabled = true;

    @Property(name = "ipv6NeighborDiscovery", boolValue = false,
            label = "Enable using IPv6 Neighbor Discovery by the " +
                    "Host Location Provider; default is false")
    private boolean ipv6NeighborDiscovery = false;

    @Property(name = "requestInterceptsEnabled", boolValue = true,
            label = "Enable requesting packet intercepts")
    private boolean requestInterceptsEnabled = true;

    protected ExecutorService eventHandler;

    private final ConfigFactory<ApplicationId, QualifiedHostProviderConfig> cfgAppFactory =
            new ConfigFactory<ApplicationId, QualifiedHostProviderConfig>(SubjectFactories.APP_SUBJECT_FACTORY,
                    QualifiedHostProviderConfig.class,
                    "qualifiedhost") {
                @Override
                public QualifiedHostProviderConfig createConfig() {
                    return new QualifiedHostProviderConfig();
                }
            };

    private final InternalConfigListener netCfgListener =
            new InternalConfigListener();

    /**
     * Creates an OpenFlow host provider.
     */
    public QualifiedHostLocationProvider() {
        super(new ProviderId("of", "org.onosproject.provider.qualifiedhost"));
    }

    @Activate
    public void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());
        appId = coreService.registerApplication("org.onosproject.provider.qualifiedhost");
        eventHandler = newSingleThreadScheduledExecutor(
                groupedThreads("onos/qualifiedhost-loc-provider", "event-handler"));
        providerService = providerRegistry.register(this);
        packetService.addProcessor(processor, PacketProcessor.advisor(1));
        deviceService.addListener(deviceListener);

        netCfgService.registerConfigFactory(cfgAppFactory);
        netCfgService.addListener(netCfgListener);

        try {
            netCfgListener.applyConfig(null);
        } catch (Exception e) {
            log.warn("Unable to start the intercepts, config probably not present yet", e);
        }

        log.info("Started with Application ID {}", appId.id());
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        netCfgService.unregisterConfigFactory(cfgAppFactory);



        providerRegistry.unregister(this);
        packetService.removeProcessor(processor);
        deviceService.removeListener(deviceListener);
        eventHandler.shutdown();
        providerService = null;

        netCfgListener.removeAllFlows();

        netCfgService.unregisterConfigFactory(cfgAppFactory);
        netCfgService.removeListener(netCfgListener);
        log.info("Stopped");
    }


    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        Boolean flag;

        flag = isPropertyEnabled(properties, "hostRemovalEnabled");
        if (flag == null) {
            log.info("Host removal on port/device down events is not configured, " +
                             "using current value of {}", hostRemovalEnabled);
        } else {
            hostRemovalEnabled = flag;
            log.info("Configured. Host removal on port/device down events is {}",
                     hostRemovalEnabled ? "enabled" : "disabled");
        }

        flag = isPropertyEnabled(properties, "ipv6NeighborDiscovery");
        if (flag == null) {
            log.info("Using IPv6 Neighbor Discovery is not configured, " +
                             "using current value of {}", ipv6NeighborDiscovery);
        } else {
            ipv6NeighborDiscovery = flag;
            log.info("Configured. Using IPv6 Neighbor Discovery is {}",
                     ipv6NeighborDiscovery ? "enabled" : "disabled");
        }

        flag = isPropertyEnabled(properties, "requestInterceptsEnabled");
        if (flag == null) {
            log.info("Request intercepts is not configured, " +
                    "using current value of {}", requestInterceptsEnabled);
        } else {
            requestInterceptsEnabled = flag;
            log.info("Configured. Request intercepts is {}",
                    requestInterceptsEnabled ? "enabled" : "disabled");
        }
    }

    /**
     * Check property name is defined and set to true.
     *
     * @param properties   properties to be looked up
     * @param propertyName the name of the property to look up
     * @return value when the propertyName is defined or return null
     */
    private static Boolean isPropertyEnabled(Dictionary<?, ?> properties,
                                             String propertyName) {
        Boolean value = null;
        try {
            String s = (String) properties.get(propertyName);
            value = isNullOrEmpty(s) ? null : s.trim().equals("true");
        } catch (ClassCastException e) {
            // No propertyName defined.
            value = null;
        }
        return value;
    }

    @Override
    public void triggerProbe(Host host) {
        log.info("Triggering probe on device {}", host);
    }

    private class InternalHostProvider implements PacketProcessor {
        /**
         * Update host location only.
         *
         * @param hid  host ID
         * @param mac  source Mac address
         * @param vlan VLAN ID
         * @param hloc host location
         */
        private void updateLocation(HostId hid, MacAddress mac,
                                    VlanId vlan, HostLocation hloc) {
            HostDescription desc = new DefaultHostDescription(mac, vlan, hloc);
            try {
                providerService.hostDetected(hid, desc, false);
            } catch (IllegalStateException e) {
                log.debug("Host {} suppressed", hid);
            }
        }

        /**
         * Update host location and IP address.
         *
         * @param hid  host ID
         * @param mac  source Mac address
         * @param vlan VLAN ID
         * @param hloc host location
         * @param ip   source IP address
         */
        private void updateLocationIP(HostId hid, MacAddress mac,
                                      VlanId vlan, HostLocation hloc,
                                      IpAddress ip) {
            HostDescription desc = ip.isZero() || ip.isSelfAssigned() ?
                    new DefaultHostDescription(mac, vlan, hloc) :
                    new DefaultHostDescription(mac, vlan, hloc, ip);
            try {
                providerService.hostDetected(hid, desc, false);
            } catch (IllegalStateException e) {
                log.debug("Host {} suppressed", hid);
            }
        }

        @Override
        public void process(PacketContext context) {
            if (context == null) {
                return;
            }

            Ethernet eth = context.inPacket().parsed();
            if (eth == null) {
                return;
            }
            MacAddress srcMac = eth.getSourceMAC();

            VlanId vlan = VlanId.vlanId(eth.getVlanID());
            ConnectPoint heardOn = context.inPacket().receivedFrom();

            // If this arrived on control port, bail out.
            if (heardOn.port().isLogical()) {
                return;
            }

            // If this is not an edge port, bail out.
            Topology topology = topologyService.currentTopology();
            if (topologyService.isInfrastructure(topology, heardOn)) {
                return;
            }

            HostLocation hloc = new HostLocation(heardOn, System.currentTimeMillis());
            HostId hid = HostId.hostId(eth.getSourceMAC(), vlan);

            // ARP: possible new hosts, update both location and IP
            if (eth.getEtherType() == Ethernet.TYPE_ARP) {
                ARP arp = (ARP) eth.getPayload();
                IpAddress ip = IpAddress.valueOf(IpAddress.Version.INET,
                                                 arp.getSenderProtocolAddress());
                updateLocationIP(hid, srcMac, vlan, hloc, ip);

            // IPv4: update location only
            } else if (eth.getEtherType() == Ethernet.TYPE_IPV4) {
                updateLocation(hid, srcMac, vlan, hloc);

            //
            // NeighborAdvertisement and NeighborSolicitation: possible
            // new hosts, update both location and IP.
            //
            // IPv6: update location only
            } else if (eth.getEtherType() == Ethernet.TYPE_IPV6) {
                IPv6 ipv6 = (IPv6) eth.getPayload();
                IpAddress ip = IpAddress.valueOf(IpAddress.Version.INET6,
                                                 ipv6.getSourceAddress());

                // skip extension headers
                IPacket pkt = ipv6;
                while (pkt.getPayload() != null &&
                        pkt.getPayload() instanceof IExtensionHeader) {
                    pkt = pkt.getPayload();
                }

                // Neighbor Discovery Protocol
                pkt = pkt.getPayload();
                if (pkt != null && pkt instanceof ICMP6) {
                    pkt = pkt.getPayload();
                    // RouterSolicitation, RouterAdvertisement
                    if (pkt != null && (pkt instanceof RouterAdvertisement ||
                            pkt instanceof RouterSolicitation)) {
                        return;
                    }
                    if (pkt != null && (pkt instanceof NeighborSolicitation ||
                            pkt instanceof NeighborAdvertisement)) {
                        // Duplicate Address Detection
                        if (ip.isZero()) {
                            return;
                        }
                        // NeighborSolicitation, NeighborAdvertisement
                        updateLocationIP(hid, srcMac, vlan, hloc, ip);
                        return;
                    }
                }

                // multicast
                if (eth.isMulticast()) {
                    return;
                }

                // normal IPv6 packets
                updateLocation(hid, srcMac, vlan, hloc);
            }
        }
    }

    // Auxiliary listener to device events.
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            eventHandler.execute(() -> handleEvent(event));
        }

        private void handleEvent(DeviceEvent event) {
            Device device = event.subject();
            switch (event.type()) {
                case DEVICE_ADDED:
                    break;
                case DEVICE_AVAILABILITY_CHANGED:
                    if (hostRemovalEnabled &&
                            !deviceService.isAvailable(device.id())) {
                        removeHosts(hostService.getConnectedHosts(device.id()));
                    }
                    break;
                case DEVICE_SUSPENDED:
                case DEVICE_UPDATED:
                    // Nothing to do?
                    break;
                case DEVICE_REMOVED:
                    if (hostRemovalEnabled) {
                        removeHosts(hostService.getConnectedHosts(device.id()));
                    }
                    break;
                case PORT_ADDED:
                    break;
                case PORT_UPDATED:
                    if (hostRemovalEnabled) {
                        ConnectPoint point =
                                new ConnectPoint(device.id(), event.port().number());
                        removeHosts(hostService.getConnectedHosts(point));
                    }
                    break;
                case PORT_REMOVED:
                    // Nothing to do?
                    break;
                default:
                    break;
            }
        }
    }

    // Signals host vanish for all specified hosts.
    private void removeHosts(Set<Host> hosts) {
        for (Host host : hosts) {
            if (host.providerId().equals(QualifiedHostLocationProvider.this.id())) {
                providerService.hostVanished(host.id());
            }
        }
    }


    private class InternalConfigListener implements NetworkConfigListener {


        HashMap<DeviceId, HashMap<VlanId, FlowRule>>  arpVlanRules;


        public InternalConfigListener() {
            arpVlanRules = new HashMap<>();
        }


        /**
         * Reads network config and initializes related data structure accordingly.
         */
        public void applyConfig(QualifiedHostProviderConfig config) {

            if (config == null) {
                //config not coming from an event, try to read it
                config = netCfgService.getConfig(appId, QualifiedHostProviderConfig.class);
                if (config == null) {
                    log.info("No configs could be found");
                    return;
                }
            }

            List<QualifiedHostProviderConfig.SwitchConfig> switchConfigs = config.switchConfigs();
            //Copy of what is already setup

            for (QualifiedHostProviderConfig.SwitchConfig switchConfig : switchConfigs) {

                DeviceId deviceId = switchConfig.getDeviceId();

                if (!arpVlanRules.containsKey(deviceId)) {

                    //New Device
                    List<VlanId> vlanIds = switchConfig.getVlans();
                    HashMap<VlanId, FlowRule> vlanRules = new HashMap<>();

                    for (VlanId vlan : vlanIds) {

                        FlowRule rule = ofdpaArpVlanFlow(deviceId, vlan);
                        vlanRules.put(vlan, rule);
                    }

                    arpVlanRules.put(deviceId, vlanRules);
                } else {
                    //Device already configured
                    HashMap<VlanId, FlowRule> deviceVlanFlows = arpVlanRules.get(deviceId);
                    HashMap<VlanId, FlowRule> copy = new HashMap<>(deviceVlanFlows);

                    List<VlanId> vlanIds = switchConfig.getVlans();

                    for (VlanId vlan : vlanIds) {
                        //Checking if the flow already exists
                        if (!deviceVlanFlows.containsKey(vlan)) {
                            //It does not exist
                            FlowRule rule = ofdpaArpVlanFlow(deviceId, vlan);
                            deviceVlanFlows.put(vlan, rule);
                        }
                        copy.remove(vlan);
                    }

                    //Remove all flows that have been removed from the config

                    for (FlowRule flowToRemove : copy.values()) {
                        flowRuleService.removeFlowRules(flowToRemove);
                    }
                }
            }
        }

        private FlowRule ofdpaArpVlanFlow(DeviceId deviceId, VlanId vlanId) {

            TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
            selector.matchEthType(Ethernet.TYPE_ARP);
            selector.extension(new OfdpaMatchVlanVid(vlanId), deviceId);

            FlowRule.Builder flow = DefaultFlowRule.builder();
            flow.withSelector(selector.build());
            flow.withTreatment(DefaultTrafficTreatment.builder().punt().build());
            flow.forTable(60);
            flow.forDevice(deviceId);
            flow.makePermanent();
            flow.fromApp(appId);
            flow.withPriority(50000);

            FlowRule rule = flow.build();
            flowRuleService.applyFlowRules(rule);

            return rule;
        }

        private void removeAllFlows() {

            for (HashMap<VlanId, FlowRule> vlanRules : arpVlanRules.values()) {
                for (FlowRule rule : vlanRules.values()) {
                    flowRuleService.removeFlowRules(rule);
                }
            }
        }

        @Override
        public void event(NetworkConfigEvent event) {
            if (event.configClass().equals(QualifiedHostProviderConfig.class)) {
                switch (event.type()) {
                    case CONFIG_ADDED:
                        log.info("Qualified Host config added");
                        applyConfig((QualifiedHostProviderConfig) event.config().get());
                        break;
                    case CONFIG_UPDATED:
                        log.info("Qualified Host config updated.");
                        applyConfig((QualifiedHostProviderConfig) event.config().get());
                        break;
                    case CONFIG_REMOVED:
                        log.info("Qualified Host config removed.");
                        removeAllFlows();
                        break;
                    case CONFIG_REGISTERED:
                        log.info("Qualified Host config registered");
                        QualifiedHostProviderConfig registeredConfig = netCfgService.getConfig(appId, QualifiedHostProviderConfig.class);
                        applyConfig(registeredConfig);
                    default:
                        break;
                }
            }
        }
    }

}
