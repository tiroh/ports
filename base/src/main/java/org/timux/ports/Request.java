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
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A class that represents an OUT port with request-response semantics.
 *
 * <p> When implementing the request type, use the {@link Response} annotation in
 * order to indicate the response type(s).
 *
 * @param <I> The type of the request data (input).
 * @param <O> The type of the response data (output).
 *
 * @see Event
 * @see Response
 *
 * @author Tim Rohlfs
 * @since 0.1
 */
public class Request<I, O> {

    private Function<I, O> port;
    private boolean isAsyncReceiver;
    private String requestTypeName;
    private Object owner;
    private String memberName;

    public Request() {
        //
    }

    Request(String requestTypeName, String memberName, Object owner) {
        this.requestTypeName = requestTypeName;
        this.memberName = memberName;
        this.owner = owner;
    }

    /**
     * Connects this OUT port to the given IN port.
     *
     * @param port The IN port that this OUT port should be connected to. Must not be null.
     */
    private synchronized void connect(Function<I, O> port, boolean isAsyncReceiver) {
        if (port == null) {
            throw new IllegalArgumentException("port must not be null");
        }

        this.port = port;
        this.isAsyncReceiver = isAsyncReceiver;
    }

    @SuppressWarnings("unchecked")
    synchronized void connect(Method portMethod, Object methodOwner) {
        if (portMethod == null) {
            throw new IllegalArgumentException("port must not be null");
        }

        Function<I, O> portFunction = x -> {
            try {
                return (O) portMethod.invoke(methodOwner, x);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        };

        connect(portFunction, portMethod.getDeclaredAnnotation(AsyncPort.class) != null);
    }

    /**
     * Disconnects this OUT port.
     */
    public void disconnect() {
        port = null;
    }

    synchronized O callWST(I payload) {
        if (Protocol.areProtocolsActive) {
            Protocol.onDataSent(requestTypeName, owner, payload);

            Function<I, O> responseProvider = (Function<I, O>) Protocol.getResponseProviderIfAvailable(requestTypeName, owner);

            if (responseProvider != null) {
                O protocolResponse = responseProvider.apply(payload);
                Protocol.onDataReceived(requestTypeName, owner, protocolResponse);
                return protocolResponse;
            }
        }

        if (port == null) {
            throw new PortNotConnectedException(memberName, owner.getClass().getName());
        }

        O response;

        try {
            response = port.apply(payload);
        } catch (Throwable t) {
            throw new ExecutionException(t);
        }

        if (Protocol.areProtocolsActive) {
            Protocol.onDataReceived(requestTypeName, owner, response);
        }

        return response;
    }

    /**
     * Sends the given payload to the connected IN port. The request will be handled synchronously, regardless of
     * whether the IN port is an {@link AsyncPort} or not.
     *
     * @param payload The payload to be sent.
     *
     * @see #submit
     * @see Ports#setAsyncPolicy
     *
     * @throws ExecutionException If the receiver terminated unexpectedly.
     * @throws PortNotConnectedException If this port is not connected.
     *
     * @return The response of the connected component.
     */
    @SuppressWarnings("unchecked")
    public O call(I payload) {
        if (MessageQueue.getAsyncPolicy() == AsyncPolicy.NO_CONTEXT_SWITCHES) {
            return callWST(payload);
        } else {
            return submit(payload).get();
        }
    }

    /**
     * Sends the given payload to the connected IN port. The request will be handled asynchronously if the IN port is
     * an {@link AsyncPort} and if the {@link AsyncPolicy} allows it; if not, the request will be handled
     * synchronously.
     *
     * @param payload The payload to be sent.
     *
     * @see #call
     * @see AsyncPort
     * @see AsyncPolicy
     *
     * @return A future of the response of the connected component. Use its {@link PortsFuture#get},
     *   {@link PortsFuture#getNow}, or {@link PortsFuture#getOrElse} methods to access the response object.
     *
     * @throws ExecutionException If the receiver terminated unexpectedly.
     * @throws PortNotConnectedException If this port is not connected.
     *
     * @since 0.5.0
     */
    @SuppressWarnings("unchecked")
    public synchronized PortsFuture<O> submit(I payload) {
        if (MessageQueue.getAsyncPolicy() == AsyncPolicy.NO_CONTEXT_SWITCHES) {
            return new PortsFuture<>(callWST(payload));
        }

        if (Protocol.areProtocolsActive) {
            Protocol.onDataSent(requestTypeName, owner, payload);

            Function<I, O> responseProvider = (Function<I, O>) Protocol.getResponseProviderIfAvailable(requestTypeName, owner);

            if (responseProvider != null) {
                O protocolResponse = responseProvider.apply(payload);
                Protocol.onDataReceived(requestTypeName, owner, protocolResponse);
                return new PortsFuture<>(protocolResponse);
            }
        }

        if (port == null) {
            throw new PortNotConnectedException(memberName, owner.getClass().getName());
        }

        if (!isAsyncReceiver) {
            O response = MessageQueue.enqueueSync(port, payload);

            if (Protocol.areProtocolsActive) {
                Protocol.onDataReceived(requestTypeName, owner, response);
            }

            return new PortsFuture<>(response);
        } else {
            Function<I, O> portFunction = x -> {
                O response = port.apply(x);

                if (Protocol.areProtocolsActive) {
                    Protocol.onDataReceived(requestTypeName, owner, response);
                }

                return response;
            };

            return MessageQueue.enqueueAsync(portFunction, payload);
        }
    }

    public Fork<O> fork(I... payloads) {
        return fork(Arrays.asList(payloads));
    }

    public Fork<O> fork(int endIndexExclusive, IntFunction<I> payloadProvider) {
        return fork(IntStream.range(0, endIndexExclusive)
                .mapToObj(payloadProvider)
                .collect(Collectors.toList()));
    }

    public Fork<O> fork(List<I> payloads) {
        Fork<O> fork = new Fork<>();

        for (I payload : payloads) {
            fork.add(submit(payload));
        }

        return fork;
    }

    /**
     * Returns true if this OUT port is connected to an IN port, false otherwise.
     */
    public boolean isConnected() {
        return port != null;
    }
}