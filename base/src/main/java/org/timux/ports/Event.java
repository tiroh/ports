/*
 * Copyright 2018-2020 Tim Rohlfs
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

package org.timux.ports;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

/**
 * A class that represents an OUT port with fire-and-forget semantics. An Event OUT port may be connected to
 * an arbitrary number of IN ports.
 *
 * @param <T> The type of the payload.
 *
 * @see Request
 *
 * @author Tim Rohlfs
 * @since 0.1
 */
public class Event<T> {

    private static class PortEntry<T> {

        Consumer<T> port;
        WeakReference<?> receiverRef;

        Domain receiverDomain;

        PortEntry(Consumer<T> port, Object receiverRef) {
            this.port = port;
            this.receiverRef = new WeakReference<>(receiverRef);
        }
    }

    private final List<PortEntry<T>> ports = new ArrayList<>(4);
    private Map<Method, Map<Object, Consumer<T>>> portMethods = null;

    private String eventTypeName;
    private Object owner;

    private int domainVersion = -1;

    public Event() {
        //
    }

    Event(String eventTypeName, Object owner) {
        this.eventTypeName = eventTypeName;
        this.owner = owner;
    }

    /**
     * Connects this OUT port to the given IN port. In case this OUT port is already connected to any IN ports,
     * the new connection will be added to the existing ones.
     *
     * @param port The IN port that this OUT port should be connected to. Must not be null.
     */
    private synchronized void connect(Consumer<T> port, Object receiver) {
        if (port == null) {
            throw new IllegalArgumentException("port must not be null");
        }

        ports.add(new PortEntry<>(port, receiver));
        domainVersion = -1;
    }

    synchronized void connect(Method portMethod, Object methodOwner, EventWrapper eventWrapper) {
        if (portMethod == null) {
            throw new IllegalArgumentException("port must not be null");
        }

        if (portMethods == null) {
            portMethods = new HashMap<>(8);
        }

        Map<Object, Consumer<T>> portOwners = portMethods.computeIfAbsent(portMethod, k -> new WeakHashMap<>(4));

        WeakReference<?> methodOwnerRef = new WeakReference<>(methodOwner);

        if (eventWrapper == null) {
            portOwners.put(
                    methodOwner,
                    x -> {
                        try {
                            Object owner = methodOwnerRef.get();

                            if (owner != null) {
                                portMethod.invoke(owner, x);
                            }
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } else {
            portOwners.put(
                    methodOwner,
                    x -> eventWrapper.execute(() -> {
                        Object owner = methodOwnerRef.get();

                        if (owner != null) {
                            try {
                                portMethod.invoke(owner, x);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }));
        }

        connect(portOwners.get(methodOwner), methodOwner);
    }

    /**
     * Connects this OUT port to the given IN port. In case this OUT port is already connected to any IN ports,
     * the new connection will be added to the existing ones.
     *
     * @param port The IN port that this OUT port should be connected to.
     */
    void connect(QueuePort<T> port, Object portOwner) {
        connect(port::add, portOwner);
    }

    /**
     * Connects this OUT port to the given IN port. In case this OUT port is already connected to any IN ports,
     * the new connection will be added to the existing ones.
     *
     * @param port The IN port that this OUT port should be connected to.
     */
    void connect(StackPort<T> port, Object portOwner) {
        connect(port::push, portOwner);
    }

    /**
     * Disconnects this OUT port from the given IN port.
     */
    synchronized void disconnect(Consumer<T> port) {
        int index = -1;

        for (int i = ports.size() - 1; i >= 0; i--) {
            if (ports.get(i).port == port) {
                index = i;
                break;
            }
        }

        if (index >= 0) {
            ports.remove(index);
            domainVersion = -1;
        }
    }

    synchronized void disconnect(Method portMethod, Object methodOwner) {
        Map<Object, Consumer<T>> portOwners = portMethods.get(portMethod);

        if (portOwners == null) {
            return;
        }

        Consumer<T> port = portOwners.remove(methodOwner);

        if (port != null) {
            disconnect(port);
        }

        if (portOwners.isEmpty()) {
            portMethods.remove(portMethod);
        }
    }

    /**
     * Sends the given payload to the connected IN port(s). Whether the event will be dispatched synchronously or
     * asynchronously or whether (and how) it will be synchronized depends on the {@link Domain}(s) of the receiver(s).
     *
     * @param payload The payload to be sent.
     *
     * @see #trigger
     * @see Domain
     */
    public void trigger(T payload) {
        if (Protocol.areProtocolsActive) {
            Protocol.onDataSent(eventTypeName, owner, payload);
        }

        final List<PortEntry<T>> p = ports;

        int i = p.size();

        if (i == 0) {
            Ports.printWarning(String.format(
                    "event %s was fired by component %s but there is no receiver",
                    eventTypeName,
                    owner.getClass().getName()));
            return;
        }

        boolean updateDomains;

        synchronized (this) {
            updateDomains = domainVersion != DomainManager.getCurrentVersion();

            if (updateDomains) {
                domainVersion = DomainManager.getCurrentVersion();
            }
        }

        for (i--; i >= 0; i--) {
            PortEntry<T> portEntry = p.get(i);

            Object receiver = portEntry.receiverRef.get();

            if (receiver == null) {
                continue;
            }

            if (updateDomains) {
                portEntry.receiverDomain = DomainManager.getDomain(receiver);
            }

            portEntry.receiverDomain.dispatch(portEntry.port, payload, receiver);
        }
    }

    private synchronized void cleanUp() {
        if (portMethods != null) {
            List<Method> garbageMethods = null;

            for (Map.Entry<Method, Map<Object, Consumer<T>>> e : portMethods.entrySet()) {
                if (e.getValue().isEmpty()) {
                    if (garbageMethods == null) {
                        garbageMethods = new ArrayList<>();
                    }

                    garbageMethods.add(e.getKey());
                }
            }

            if (garbageMethods != null) {
                garbageMethods.forEach(portMethods::remove);
            }
        }

        List<PortEntry<T>> garbagePortEntries = null;

        for (PortEntry<T> portEntry : ports) {
            if (portEntry.receiverRef.get() == null) {
                if (garbagePortEntries == null) {
                    garbagePortEntries = new ArrayList<>();
                }

                garbagePortEntries.add(portEntry);
            }
        }

        if (garbagePortEntries != null) {
            ports.removeAll(garbagePortEntries);
        }
    }

    /**
     * Returns true if this OUT port is connected to an IN port, false otherwise.
     */
    public synchronized boolean isConnected() {
        cleanUp();
        return !ports.isEmpty();
    }
}
