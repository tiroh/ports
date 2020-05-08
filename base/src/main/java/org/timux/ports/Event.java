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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    // FIXME make this thread-safe

    private static class PortEntry<T> {

        Consumer<T> port;
        boolean isAsyncReceiver;

        PortEntry(Consumer<T> port, boolean isAsyncReceiver) {
            this.port = port;
            this.isAsyncReceiver = isAsyncReceiver;
        }
    }

    private final List<PortEntry<T>> ports = new ArrayList<>(4);
    private Map<Method, Map<WeakReference<?>, Consumer<T>>> portMethods = null;
    private PortEntry<T> singlePort = null;
    private String eventTypeName;
    private Object owner;

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
    void connect(Consumer<T> port, boolean isAsyncReceiver) {
        if (port == null) {
            throw new IllegalArgumentException("port must not be null");
        }

        ports.add(new PortEntry<>(port, isAsyncReceiver));

        singlePort = ports.size() == 1
                ? ports.get(0)
                : null;
    }

    void connect(Method portMethod, Object methodOwner) {
        connect(portMethod, methodOwner, null);
    }

    void connect(Method portMethod, Object methodOwner, EventWrapper eventWrapper) {
        if (portMethod == null) {
            throw new IllegalArgumentException("port must not be null");
        }

        cleanUpGarbageCollectedConnections();

        if (portMethods == null) {
            portMethods = new HashMap<>();
        }

        Map<WeakReference<?>, Consumer<T>> portOwners = portMethods.computeIfAbsent(portMethod, k -> new HashMap<>(4));

        WeakReference<?> key = new WeakReference<>(methodOwner);

        if (eventWrapper == null) {
            portOwners.put(
                    key,
                    x -> {
                        try {
                            Object owner = key.get();

                            if (owner != null) {
                                portMethod.invoke(owner, x);
                            }
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } else {
            portOwners.put(
                    key,
                    x -> eventWrapper.execute(() -> {
                        Object owner = key.get();

                        if (owner != null) {
                            try {
                                portMethod.invoke(owner, x);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }));
        }

        connect(portOwners.get(key), portMethod.getDeclaredAnnotation(AsyncPort.class) != null);
    }

    /**
     * Connects this OUT port to the given IN port. In case this OUT port is already connected to any IN ports,
     * the new connection will be added to the existing ones.
     *
     * @param port The IN port that this OUT port should be connected to.
     */
    void connect(Queue<T> port) {
        connect(port::add, false);
    }

    /**
     * Connects this OUT port to the given IN port. In case this OUT port is already connected to any IN ports,
     * the new connection will be added to the existing ones.
     *
     * @param port The IN port that this OUT port should be connected to.
     */
    void connect(Stack<T> port) {
        connect(port::push, false);
    }

    /**
     * Disconnects this OUT port from the given IN port.
     */
    void disconnect(Consumer<T> port) {
        int index = -1;

        for (int i = ports.size() - 1; i >= 0; i--) {
            if (ports.get(i).port == port) {
                index = i;
                break;
            }
        }

        if (index >= 0) {
            ports.remove(index);

            singlePort = ports.size() == 1
                    ? ports.get(0)
                    : null;
        }
    }

    void disconnect(Method portMethod, Object methodOwner) {
        Map<WeakReference<?>, Consumer<T>> portOwners = portMethods.get(portMethod);

        if (portOwners == null) {
            return;
        }

        WeakReference<?> key = getPortOwnersKeyOf(methodOwner, portOwners);

        if (key != null) {
            disconnect(portOwners.get(key));
        }

        portOwners.remove(key);

        if (portOwners.isEmpty()) {
            portMethods.remove(portMethod);
        }
    }

    private WeakReference<?> getPortOwnersKeyOf(Object portOwner, Map<WeakReference<?>, Consumer<T>> portOwners) {
        for (Map.Entry<WeakReference<?>, Consumer<T>> e : portOwners.entrySet()) {
            if (e.getKey().get() == portOwner) {
                return e.getKey();
            }
        }

        // owner has been garbage-collected.
        return null;
    }

    private void cleanUpGarbageCollectedConnections() {
        if (portMethods == null || portMethods.isEmpty()) {
            return;
        }

        Map<WeakReference<?>, Method> collectedReferences = new HashMap<>();

        for (Map.Entry<Method, Map<WeakReference<?>, Consumer<T>>> e : portMethods.entrySet()) {
            for (Map.Entry<WeakReference<?>, Consumer<T>> ee : e.getValue().entrySet()) {
                if (ee.getKey().get() == null) {
                    collectedReferences.put(ee.getKey(), e.getKey());
                }
            }
        }

        for (Map.Entry<WeakReference<?>, Method> e : collectedReferences.entrySet()) {
            WeakReference<?> reference = e.getKey();
            Method method = e.getValue();

            disconnect(portMethods.get(method).get(reference));

            portMethods.get(method).remove(reference);

            if (portMethods.get(method).isEmpty()) {
                portMethods.remove(method);
            }
        }
    }

    void triggerWithinSameThread(T payload) {
        if (Protocol.areProtocolsActive) {
            Protocol.onDataSent(eventTypeName, owner, payload);
        }

        if (singlePort != null) {
            singlePort.port.accept(payload);
            return;
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

        for (i--; i >= 0; i--) {
            p.get(i).port.accept(payload);
        }
    }

    /**
     * Sends the given payload to the connected IN port(s). For each IN port, the event will be handled asynchronously
     * if the IN port is an {@link AsyncPort} and if the {@link AsyncPolicy} allows it; if not, the event will be
     * handled synchronously, but not necessarily within the thread of the caller.
     *
     * @param payload The payload to be sent.
     *
     * @see #trigger
     * @see AsyncPort
     * @see AsyncPolicy
     *
     * @since 0.5.0
     */
    public void trigger(T payload) {
        if (MessageQueue.getAsyncPolicy() == AsyncPolicy.NO_CONTEXT_SWITCHES) {
            triggerWithinSameThread(payload);
            return;
        }

        if (Protocol.areProtocolsActive) {
            Protocol.onDataSent(eventTypeName, owner, payload);
        }

        if (singlePort != null) {
            if (singlePort.isAsyncReceiver) {
                MessageQueue.enqueueAsync(singlePort.port, payload);
            } else {
                MessageQueue.enqueue(singlePort.port, payload);
            }

            return;
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

        for (i--; i >= 0; i--) {
            if (p.get(i).isAsyncReceiver) {
                MessageQueue.enqueueAsync(p.get(i).port, payload);
            } else {
                MessageQueue.enqueue(p.get(i).port, payload);
            }
        }
    }

    /**
     * Returns true if this OUT port is connected to an IN port, false otherwise.
     */
    public boolean isConnected() {
        cleanUpGarbageCollectedConnections();
        return !ports.isEmpty();
    }
}
