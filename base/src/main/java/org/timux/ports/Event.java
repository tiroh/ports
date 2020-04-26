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

    private final List<Consumer<T>> ports = new ArrayList<>();
    private Map<Method, Map<Object, Consumer<T>>> portMethods = null;
    private Consumer<T> singlePort = null;
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
    protected void connect(Consumer<T> port) {
        if (port == null) {
            throw new IllegalArgumentException("port must not be null");
        }

        ports.add(port);

        singlePort = ports.size() == 1
                ? port
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

        connect(portOwners.get(methodOwner));
    }

    /**
     * Connects this OUT port to the given IN port. In case this OUT port is already connected to any IN ports,
     * the new connection will be added to the existing ones.
     *
     * @param port The IN port that this OUT port should be connected to.
     */
    public void connect(Queue<T> port) {
        connect(port::add);
    }

    /**
     * Connects this OUT port to the given IN port. In case this OUT port is already connected to any IN ports,
     * the new connection will be added to the existing ones.
     *
     * @param port The IN port that this OUT port should be connected to.
     */
    public void connect(Stack<T> port) {
        connect(port::push);
    }

    /**
     * Disconnects this OUT port from the given IN port.
     */
    public void disconnect(Object port) {
        ports.remove(port);

        singlePort = ports.size() == 1
                ? ports.get(0)
                : null;
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
     * Sends the given payload to the connected IN port(s).
     *
     * @param payload The payload to be sent.
     */
    public void trigger(T payload) {
        if (Protocol.areProtocolsActive) {
            Protocol.onDataSent(eventTypeName, owner, payload);
        }

        if (singlePort != null) {
            singlePort.accept(payload);
            return;
        }

        final List<Consumer<T>> p = ports;

        int i = p.size();

        if (i == 0) {
            System.err.println(String.format(
                    "[ports] warning: event %s was fired by component %s but there is no receiver",
                    eventTypeName,
                    owner.getClass().getName()));
            return;
        }

        for (i--; i >= 0; i--) {
            p.get(i).accept(payload);
        }
    }

    /**
     * Returns true if this OUT port is connected to an IN port, false otherwise.
     */
    public boolean isConnected() {
        return !ports.isEmpty();
    }
}
