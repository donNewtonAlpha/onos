/*
 * Copyright 2015,2016 Open Networking Laboratory
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
package org.onosproject.ui.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ShortNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.mastership.MastershipService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiMessageHandlerFactory;
import org.onosproject.ui.UiPreferencesService;
import org.onosproject.ui.UiTopoOverlayFactory;
import org.onosproject.ui.UiView;
import org.onosproject.ui.UiViewHidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.ImmutableList.of;
import static java.util.stream.Collectors.toSet;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.UI_READ;
import static org.onosproject.security.AppPermission.Type.UI_WRITE;
import static org.onosproject.ui.UiView.Category.NETWORK;
import static org.onosproject.ui.UiView.Category.PLATFORM;

/**
 * Manages the user interface extensions.
 */
@Component(immediate = true)
@Service
public class UiExtensionManager implements UiExtensionService, UiPreferencesService, SpriteService {

    private static final ClassLoader CL = UiExtensionManager.class.getClassLoader();
    private static final String CORE = "core";
    private static final String GUI_ADDED = "guiAdded";
    private static final String GUI_REMOVED = "guiRemoved";
    private static final String UPDATE_PREFS = "updatePrefs";

    private final Logger log = LoggerFactory.getLogger(getClass());

    // List of all extensions
    private final List<UiExtension> extensions = Lists.newArrayList();

    // Map of views to extensions
    private final Map<String, UiExtension> views = Maps.newHashMap();

    // Core views & core extension
    private final UiExtension core = createCoreExtension();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    // User preferences
    private EventuallyConsistentMap<String, ObjectNode> prefs;
    private final EventuallyConsistentMapListener<String, ObjectNode> prefsListener =
            new InternalPrefsListener();

    private final ObjectMapper mapper = new ObjectMapper();

    // Creates core UI extension
    private UiExtension createCoreExtension() {
        List<UiView> coreViews = of(
                new UiView(PLATFORM, "app", "Applications", "nav_apps"),
                new UiView(PLATFORM, "settings", "Settings", "nav_settings"),
                new UiView(PLATFORM, "cluster", "Cluster Nodes", "nav_cluster"),
                new UiView(PLATFORM, "processor", "Packet Processors", "nav_processors"),
                new UiView(NETWORK, "topo", "Topology", "nav_topo"),
                new UiView(NETWORK, "device", "Devices", "nav_devs"),
                new UiViewHidden("flow"),
                new UiViewHidden("port"),
                new UiViewHidden("group"),
                new UiViewHidden("meter"),
                new UiView(NETWORK, "link", "Links", "nav_links"),
                new UiView(NETWORK, "host", "Hosts", "nav_hosts"),
                new UiView(NETWORK, "intent", "Intents", "nav_intents"),
                //TODO add a new type of icon for tunnel
                new UiView(NETWORK, "tunnel", "Tunnels", "nav_links")
        );

        UiMessageHandlerFactory messageHandlerFactory =
                () -> ImmutableList.of(
                        new UserPreferencesMessageHandler(),
                        new TopologyViewMessageHandler(),
                        new DeviceViewMessageHandler(),
                        new LinkViewMessageHandler(),
                        new HostViewMessageHandler(),
                        new FlowViewMessageHandler(),
                        new PortViewMessageHandler(),
                        new GroupViewMessageHandler(),
                        new MeterViewMessageHandler(),
                        new IntentViewMessageHandler(),
                        new ApplicationViewMessageHandler(),
                        new SettingsViewMessageHandler(),
                        new ClusterViewMessageHandler(),
                        new ProcessorViewMessageHandler(),
                        new TunnelViewMessageHandler()
                );

        UiTopoOverlayFactory topoOverlayFactory =
                () -> ImmutableList.of(
                        new TrafficOverlay()
                );

        return new UiExtension.Builder(CL, coreViews)
                .messageHandlerFactory(messageHandlerFactory)
                .topoOverlayFactory(topoOverlayFactory)
                .resourcePath(CORE)
                .build();
    }

    @Activate
    public void activate() {
        KryoNamespace.Builder kryoBuilder = new KryoNamespace.Builder()
                .register(KryoNamespaces.API)
                .register(ObjectNode.class, ArrayNode.class,
                          JsonNodeFactory.class, LinkedHashMap.class,
                          TextNode.class, BooleanNode.class,
                          LongNode.class, DoubleNode.class, ShortNode.class,
                          IntNode.class, NullNode.class);

        prefs = storageService.<String, ObjectNode>eventuallyConsistentMapBuilder()
                .withName("onos-user-preferences")
                .withSerializer(kryoBuilder)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .withPersistence()
                .build();
        prefs.addListener(prefsListener);
        register(core);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        prefs.removeListener(prefsListener);
        UiWebSocketServlet.closeAll();
        unregister(core);
        log.info("Stopped");
    }

    @Override
    public synchronized void register(UiExtension extension) {
        checkPermission(UI_WRITE);
        if (!extensions.contains(extension)) {
            extensions.add(extension);
            for (UiView view : extension.views()) {
                views.put(view.id(), extension);
            }
            UiWebSocketServlet.sendToAll(GUI_ADDED, null);
        }
    }

    @Override
    public synchronized void unregister(UiExtension extension) {
        checkPermission(UI_WRITE);
        extensions.remove(extension);
        extension.views().stream().map(UiView::id).collect(toSet()).forEach(views::remove);
        UiWebSocketServlet.sendToAll(GUI_REMOVED, null);
    }

    @Override
    public synchronized List<UiExtension> getExtensions() {
        checkPermission(UI_READ);
        return ImmutableList.copyOf(extensions);
    }

    @Override
    public synchronized UiExtension getViewExtension(String viewId) {
        checkPermission(UI_READ);
        return views.get(viewId);
    }

    @Override
    public Set<String> getUserNames() {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        prefs.keySet().forEach(k -> builder.add(userName(k)));
        return builder.build();
    }

    @Override
    public Map<String, ObjectNode> getPreferences(String userName) {
        ImmutableMap.Builder<String, ObjectNode> builder = ImmutableMap.builder();
        prefs.entrySet().stream()
                .filter(e -> e.getKey().startsWith(userName + "/"))
                .forEach(e -> builder.put(keyName(e.getKey()), e.getValue()));
        return builder.build();
    }

    @Override
    public void setPreference(String userName, String preference, ObjectNode value) {
        prefs.put(key(userName, preference), value);
    }

    // =====================================================================
    // Provisional tracking of sprite definitions

    private final Map<String, JsonNode> sprites = Maps.newHashMap();

    @Override
    public Set<String> getNames() {
        return ImmutableSet.copyOf(sprites.keySet());
    }

    @Override
    public void put(String name, JsonNode spriteData) {
        log.info("Registered sprite definition [{}]", name);
        sprites.put(name, spriteData);
    }

    @Override
    public JsonNode get(String name) {
        return sprites.get(name);
    }

    private String key(String userName, String keyName) {
        return userName + "/" + keyName;
    }

    private String userName(String key) {
        return key.split("/")[0];
    }

    private String keyName(String key) {
        return key.split("/")[1];
    }

    // Auxiliary listener to preference map events.
    private class InternalPrefsListener
            implements EventuallyConsistentMapListener<String, ObjectNode> {
        @Override
        public void event(EventuallyConsistentMapEvent<String, ObjectNode> event) {
            String userName = userName(event.key());
            if (event.type() == EventuallyConsistentMapEvent.Type.PUT) {
                UiWebSocketServlet.sendToUser(userName, UPDATE_PREFS, jsonPrefs());
            }
        }

        private ObjectNode jsonPrefs() {
            ObjectNode json = mapper.createObjectNode();
            prefs.entrySet().forEach(e -> json.set(keyName(e.getKey()), e.getValue()));
            return json;
        }
    }
}
