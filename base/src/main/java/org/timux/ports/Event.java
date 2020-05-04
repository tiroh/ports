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

    private static class PortEntry<T> {

        Consumer<T> port;
        boolean isAsyncReceiver;

        PortEntry(Consumer<T> port, boolean isAsyncReceiver) {
            this.port = port;
            this.isAsyncReceiver = isAsyncReceiver;
        }
    }

    private final List<PortEntry<T>> ports = new ArrayList<>();
    private Map<Method, Map<Object, Consumer<T>>> portMethods = null;
    private PortEntry<T> singlePort = null;
    private String eventTypeName;
    private Object owner;

    public Event() {
        //
    }

    protected Event(String eventTypeName, Object owner) {
        this.eventTypeName = eventTypeName;
        this.owner = owner;
    }

    /**
     * Connects this OUT port to the given IN port. In case this OUT port is already connected to any IN ports,
     * the new connection will be added to the existing ones.
     *
     * @param port The IN port that this OUT port should be connected to. Must not be null.
     */
    protected void connect(Consumer<T> port, boolean isAsyncReceiver) {
        if (port == null) {
            throw new IllegalArgumentException("port must not be null");
        }

        ports.add(new PortEntry<>(port, isAsyncReceiver));

        singlePort = ports.size() == 1
                ? ports.get(0)
                : null;
    }

    protected void connect(Method portMethod, Object methodOwner) {
        connect(portMethod, methodOwner, null);
    }

    protected void connect(Method portMethod, Object methodOwner, EventWrapper eventWrapper) {
        if (portMethod == null) {
            throw new IllegalArgumentException("port must not be null");
        }

        if (portMethods == null) {
            portMethods = new HashMap<>();
        }

        Map<Object, Consumer<T>> portOwners = portMethods.computeIfAbsent(portMethod, k -> new HashMap<>(4));

        if (eventWrapper == null) {
            portOwners.put(
                    methodOwner,
                    x -> {
                        try {
                            portMethod.invoke(methodOwner, x);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } else {
            portOwners.put(
                    methodOwner,
                    x -> eventWrapper.execute(() -> {
                        try {
                            portMethod.invoke(methodOwner, x);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    }));
        }

        connect(portOwners.get(methodOwner), portMethod.getDeclaredAnnotation(AsyncPort.class) != null);
    }

    /**
     * Connects this OUT port to the given IN port. In case this OUT port is already connected to any IN ports,
     * the new connection will be added to the existing ones.
     *
     * @param port The IN port that this OUT port should be connected to.
     */
    public void connect(Queue<T> port) {
        connect(port::add, false);
    }

    /**
     * Connects this OUT port to the given IN port. In case this OUT port is already connected to any IN ports,
     * the new connection will be added to the existing ones.
     *
     * @param port The IN port that this OUT port should be connected to.
     */
    public void connect(Stack<T> port) {
        connect(port::push, false);
    }

    /**
     * Disconnects this OUT port from the given IN port.
     */
    public void disconnect(Object port) {
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

    protected void disconnect(Method portMethod, Object methodOwner) {
        Map<Object, Consumer<T>> portOwners = portMethods.get(portMethod);

        if (portOwners == null) {
            return;
        }

        disconnect(portOwners.get(methodOwner));

        portOwners.remove(methodOwner);

        if (portOwners.isEmpty()) {
            portMethods.remove(portMethod);
        }
    }

    /**
     * Sends the given payload to the connected IN port(s). The event will be handled synchronously, regardless of
     * whether the IN port is an {@link AsyncPort} or not.
     *
     * @param payload The payload to be sent.
     *                
     * @see #triggerAsync
     */
    public void trigger(T payload) {
        if (Protocol.areProtocolsActive) {
            Protocol.onDataSent(eventTypeName, owner, payload);
        }

        if (singlePort != null) {
            MessageQueue.enqueue(singlePort.port, payload);
            return;
        }

        final List<PortEntry<T>> p = ports;

        int i = p.size();

        if (i == 0) {
            System.err.println(String.format(
                    "[ports] warning: event %s was fired by component %s but there is no receiver",
                    eventTypeName,
                    owner.getClass().getName()));
            return;
        }

        for (i--; i >= 0; i--) {
            MessageQueue.enqueue(p.get(i).port, payload);
        }
    }

    /**
     * Attempts to send the given payload asynchronously to the connected IN port(s). If the IN port is not an
     * {@link AsyncPort}, the event will be handled synchronously.
     *
     * @param payload The payload to be sent.
     *
     * @see AsyncPort
     *
     * @since 0.5.0
     */
    public void triggerAsync(T payload) {
        if (Protocol.areProtocolsActive) {
            Protocol.onDataSent(eventTypeName, owner, payload);
        }

        if (singlePort != null) {
            if (singlePort.isAsyncReceiver) {
                MessageQueue.enqueueAsync(singlePort.port, payload);
            } else {
                System.err.println(String.format(
                        "[ports] warning: event %s was fired asynchronously in component %s, but the receiver is not an async port",
                        eventTypeName,
                        owner.getClass().getName()));
                MessageQueue.enqueue(singlePort.port, payload);
            }
            return;
        }

        final List<PortEntry<T>> p = ports;

        int i = p.size();

        if (i == 0) {
            System.err.println(String.format(
                    "[ports] warning: event %s was fired by component %s but there is no receiver",
                    eventTypeName,
                    owner.getClass().getName()));
            return;
        }

        for (i--; i >= 0; i--) {
            if (p.get(i).isAsyncReceiver) {
                MessageQueue.enqueueAsync(p.get(i).port, payload);
            } else {
                System.err.println(String.format(
                        "[ports] warning: event %s was fired asynchronously in component %s, but the receiver is not an async port",
                        eventTypeName,
                        owner.getClass().getName()));
                MessageQueue.enqueue(p.get(i).port, payload);
            }
        }
    }

    /**
     * Returns true if this OUT port is connected to an IN port, false otherwise.
     */
    public boolean isConnected() {
        return !ports.isEmpty();
    }
}
