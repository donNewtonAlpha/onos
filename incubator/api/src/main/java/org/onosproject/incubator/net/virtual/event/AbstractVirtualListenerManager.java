/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.incubator.net.virtual.event;

import org.onosproject.event.Event;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.event.EventListener;
import org.onosproject.event.ListenerService;
import org.onosproject.incubator.net.virtual.NetworkId;

/**
 * Basis for virtual event components which need to export listener mechanism.
 */
public abstract class AbstractVirtualListenerManager
        <E extends Event, L extends EventListener<E>>
    implements ListenerService<E, L> {

    protected final NetworkId networkId;

    protected EventDeliveryService eventDispatcher;

    VirtualListenerRegistryManager listenerManager =
            VirtualListenerRegistryManager.getInstance();

    public AbstractVirtualListenerManager(NetworkId networkId) {
        this.networkId = networkId;
    }

    @Override
    public void addListener(L listener) {
        listenerManager.getRegistry(networkId, getEventClass())
                .addListener(listener);
    }

    @Override
    public void removeListener(L listener) {
        listenerManager.getRegistry(networkId, getEventClass())
                .removeListener(listener);
    }

    /**
     * Safely posts the specified event to the local event dispatcher.
     * If there is no event dispatcher or if the event is null, this method
     * is a noop.
     *
     * @param event event to be posted; may be null
     */
    protected void post(E event) {
        if (event != null && eventDispatcher != null) {
            VirtualEvent<E> vEvent =
                    new VirtualEvent<E>(networkId, VirtualEvent.Type.POSTED, event);
            eventDispatcher.post(vEvent);
        }
    }

    /**
     * Returns the class type of parameter type.
     * More specifically, it returns the class type of event class.
     *
     * @return the class type of provider service of the service
     */
    private Class getEventClass() {
        String className = this.getClass().getGenericSuperclass().toString();
        String pramType = className.split("<")[1].split(",")[0];

        try {
            return Class.forName(pramType);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

}
