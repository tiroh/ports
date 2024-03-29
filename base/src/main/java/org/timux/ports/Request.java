/*
 * Copyright 2018-2021 Tim Rohlfs
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

import org.timux.ports.types.Either;
import org.timux.ports.types.Either3;
import org.timux.ports.types.Failure;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
 * @author Tim Rohlfs
 * @see Event
 * @see Response
 * @since 0.1
 */
public class Request<I, O> {

    private Function<I, O> port;
    private String requestTypeName;
    private Object owner;
    private Object receiver;
    private String memberName;

    private PortsFutureResponseTypeInfo responseTypeInfo;

    private Domain receiverDomain;
    private Function<I, O> wrappedFunction;
    private int domainVersion = -1;

    private final RequestCache<I, PortsFuture<O>> cache;

    public Request() {
        cache = null;
    }

    Request(String requestTypeName, String responseTypeName, String memberName, Object owner) {
        this.requestTypeName = requestTypeName;
        this.memberName = memberName;
        this.owner = owner;
        this.responseTypeInfo = getResponseTypeInfo(responseTypeName);

        try {
            Class<?> requestType = getClass().getClassLoader().loadClass(requestTypeName);
            Pure pureAnno = requestType.getDeclaredAnnotation(Pure.class);
            boolean isCacheEnabled = pureAnno != null && pureAnno.cache();
            this.cache = isCacheEnabled ? new RequestCache<>(4, requestType) : null;

            if (isCacheEnabled) {
                CacheManager.registerRequestPort(this, pureAnno);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    Request(String requestTypeName, Field outPortField, String memberName, Object owner) {
        this(requestTypeName, getResponseTypeName(outPortField), memberName, owner);
    }

    public String getRequestTypeName() {
        return requestTypeName;
    }

    private static String getResponseTypeName(Field outPortField) {
        String t = TypeUtils.extractTypeParameter(outPortField.getGenericType().getTypeName(), null);
        int firstSpacePos = t.indexOf(' ');
        return t.substring(firstSpacePos + 1);
    }

    /**
     * Connects this OUT port to the given IN port.
     *
     * @param port The IN port that this OUT port should be connected to. Must not be null.
     */
    private synchronized void connect(Function<I, O> port, Object receiver) {
        if (port == null) {
            throw new IllegalArgumentException("port must not be null");
        }

        this.port = port;
        this.receiver = receiver;
        this.domainVersion = -1;
    }

    @SuppressWarnings("unchecked")
    synchronized void connect(Method portMethod, Object methodOwner) {
        if (portMethod == null) {
            throw new IllegalArgumentException("port must not be null");
        }

        Function<I, O> portFunction = x -> {
            try {
                return (O) portMethod.invoke(methodOwner, x);
            } catch (IllegalAccessException e) {
                throw new PortsExecutionException(e);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof PortsExecutionException) {
                    throw (PortsExecutionException) e.getCause();
                }

                throw new PortsExecutionException(e.getCause());
            }
        };

        connect(portFunction, methodOwner);
    }

    private PortsFutureResponseTypeInfo getResponseTypeInfo(String responseTypeName) {
        if (responseTypeName.startsWith(Either.class.getName() + "<")) {
            if (responseTypeName.endsWith(" " + Failure.class.getName() + ">")) {
                return PortsFutureResponseTypeInfo.EITHER_X_FAILURE;
            } else {
                return PortsFutureResponseTypeInfo.OTHER;
            }
        }

        if (responseTypeName.startsWith(Either3.class.getName() + "<")) {
            if (responseTypeName.endsWith(" " + Failure.class.getName() + ">")) {
                return PortsFutureResponseTypeInfo.EITHER3_X_Y_FAILURE;
            } else {
                return PortsFutureResponseTypeInfo.OTHER;
            }
        }

        return PortsFutureResponseTypeInfo.OTHER;
    }

    /**
     * Disconnects this OUT port.
     */
    public void disconnect() {
        port = null;
        receiver = null;
        receiverDomain = null;
        domainVersion = -1;
        wrappedFunction = null;
    }

    /**
     * Sends the given payload to the connected IN port. The request will be handled synchronously, but not
     * necessarily within the thread of the sender (this depends on the {@link Domain} of the receiver).
     *
     * @param payload The payload to be sent.
     * @return The response of the receiver.
     * @throws PortsExecutionException   If the receiver terminated unexpectedly.
     * @throws PortNotConnectedException If this port is not connected.
     * @see #callF
     * @see Domain
     */
    @SuppressWarnings("unchecked")
    public O call(I payload) {
        CacheManager.onMessageSent(payload.getClass());

        PortsFuture<O> cachedFuture = cache != null ? cache.get(payload) : null;

        if (cachedFuture != null) {
            if (Protocol.areProtocolsActive) {
                try {
                    Protocol.onDataSent(requestTypeName, owner, payload);
                } catch (Exception e) {
                    /*
                     * When the protocol event handler crashes, this doesn't mean that the
                     * request itself has a problem, so we shouldn't terminate here. We just
                     * log the stack trace and continue.
                     */
                    e.printStackTrace();
                }

                try {
                    Protocol.onDataReceived(requestTypeName, owner, cachedFuture);
                } catch (Exception e) {
                    /*
                     * Again: When the protocol event handler crashes, we should just log the
                     * stack trace and continue normally.
                     */
                    e.printStackTrace();
//                    return new PortsFuture<O>(e, responseTypeInfo).get();
                }
            }

            return cachedFuture.get();
        }

        PortsFuture<O> future = callF_internal(payload);
        O response = future.get();

        if (Protocol.areProtocolsActive && future.hasExceptionOccurred()) {
            Protocol.onDataReceived(requestTypeName, owner, response);
        }

        if (cache != null) {
            if (future.hasExceptionOccurred()) {
                return response;
            }

            switch (responseTypeInfo) {
            case EITHER_X_FAILURE:
                if (((Either<?, Failure>) response).isFailure()) {
                    return response;
                }
                break;

            case EITHER3_X_Y_FAILURE:
                if (((Either3<?, ?, Failure>) response).isFailure()) {
                    return response;
                }
                break;

            case OTHER:
                break;

            default:
                throw new IllegalStateException("unhandled type info: " + responseTypeInfo);
            }

            cache.put(payload, future);
        }

        return response;
    }

    /**
     * Sends the given payload to the connected IN port. The request will be handled synchronously, but not
     * necessarily within the thread of the sender (this depends on the {@link Domain} of the receiver).
     *
     * @param payload The payload to be sent.
     * @return An {@link Either} containing either the response of the receiver or a
     * {@link Failure} in case the receiver terminated with an exception.
     * @throws PortNotConnectedException If this port is not connected.
     * @see #callF
     * @see Domain
     */
    public Either<O, Failure> callE(I payload) {
        CacheManager.onMessageSent(payload.getClass());

        PortsFuture<O> cachedFuture = cache != null ? cache.get(payload) : null;

        if (cachedFuture != null) {
            if (Protocol.areProtocolsActive) {
                try {
                    Protocol.onDataSent(requestTypeName, owner, payload);
                } catch (Exception e) {
                    /*
                     * When the protocol event handler crashes, this doesn't mean that the
                     * request itself has a problem, so we shouldn't terminate here. We just
                     * log the stack trace and continue.
                     */
                    e.printStackTrace();
                }

                try {
                    Protocol.onDataReceived(requestTypeName, owner, cachedFuture);
                } catch (Exception e) {
                    /*
                     * Again: When the protocol event handler crashes, we should just log the
                     * stack trace and continue normally.
                     */
                    e.printStackTrace();
//                    return new PortsFuture<O>(e, responseTypeInfo).get();
                }
            }

            return cachedFuture.getE();
        }

        PortsFuture<O> future = callF_internal(payload);
        Either<O, Failure> response = future.getE();

        if (Protocol.areProtocolsActive && future.hasExceptionOccurred()) {
            Protocol.onDataReceived(requestTypeName, owner, response);
        }

        if (cache != null) {
            if (response.isFailure()) {
                return response;
            }

            cache.put(payload, future);
        }

        return response;
    }

    /**
     * Sends the given payload to the connected IN port. Whether the request will be dispatched synchronously or
     * asynchronously or whether (and how) it will be synchronized depends on the {@link Domain} of the receiver.
     *
     * @param payload The payload to be sent.
     * @return A future of the response of the receiver. Use its {@link PortsFuture#get},
     * {@link PortsFuture#getNow}, or {@link PortsFuture#getE} methods to access the response object.
     * @throws PortNotConnectedException If this port is not connected.
     * @see #call
     * @see Domain
     * @since 0.5.0
     */
    @SuppressWarnings("unchecked")
    public PortsFuture<O> callF(I payload) {
        CacheManager.onMessageSent(payload.getClass());

        PortsFuture<O> cachedFuture = cache != null ? cache.get(payload) : null;

        if (cachedFuture != null) {
            if (Protocol.areProtocolsActive) {
                try {
                    Protocol.onDataSent(requestTypeName, owner, payload);
                } catch (Exception e) {
                    /*
                     * When the protocol event handler crashes, this doesn't mean that the
                     * request itself has a problem, so we shouldn't terminate here. We just
                     * log the stack trace and continue.
                     */
                    e.printStackTrace();
                }

                try {
                    Protocol.onDataReceived(requestTypeName, owner, cachedFuture);
                } catch (Exception e) {
                    /*
                     * Again: When the protocol event handler crashes, we should just log the
                     * stack trace and continue normally.
                     */
                    e.printStackTrace();
//                    return new PortsFuture<O>(e, responseTypeInfo).get();
                }
            }

            return cachedFuture;
        }

        PortsFuture<O> future = callF_internal(payload);

        if (cache != null) {
            O maybeResponse = future.getNow(null);

            if (!future.hasExceptionOccurred() && maybeResponse != null) {
                switch (responseTypeInfo) {
                case EITHER_X_FAILURE:
                    if (((Either<?, Failure>) maybeResponse).isFailure()) {
                        return future;
                    }
                    break;

                case EITHER3_X_Y_FAILURE:
                    if (((Either3<?, ?, Failure>) maybeResponse).isFailure()) {
                        return future;
                    }
                    break;

                case OTHER:
                    break;

                default:
                    throw new IllegalStateException("unhandled type info: " + responseTypeInfo);
                }

                cache.put(payload, future);
            }

            return future;
        }

        return future;
    }

    private PortsFuture<O> callF_internal(I payload) {
        if (Protocol.areProtocolsActive) {
            try {
                Protocol.onDataSent(requestTypeName, owner, payload);

                Function<I, O> responseProvider = (Function<I, O>) Protocol.getResponseProviderIfAvailable(requestTypeName, owner);

                if (responseProvider != null) {
                    O protocolResponse = responseProvider.apply(payload);
                    Protocol.onDataReceived(requestTypeName, owner, protocolResponse);
                    return new PortsFuture<>(protocolResponse);
                }
            } catch (Exception e) {
                return new PortsFuture<>(e, responseTypeInfo);
            }
        }

        if (port == null) {
            throw new PortNotConnectedException(memberName, owner.getClass().getName());
        }

        synchronized (this) {
            if (domainVersion != DomainManager.getCurrentVersion()) {
                domainVersion = DomainManager.getCurrentVersion();
                receiverDomain = DomainManager.getDomain(receiver);
                wrappedFunction = getWrappedFunctionForProtocols();
            }
        }

        return receiverDomain.dispatch(wrappedFunction, payload, owner, receiver, responseTypeInfo);
    }

    private Function<I, O> getWrappedFunctionForProtocols() {
        return Protocol.areProtocolsActive
                ? (x -> {
            O response = port.apply(x);

            /*
             * The following call only handles the "happy case", i.e. that no exception occurred.
             * If the 'apply' call crashed with an exception, we won't reach his point. Instead, the exception
             * will be caught within the Task class. In the 'call' and 'callE' methods above, it will be
             * extracted from the returned future and relayed to the receiver (if the response type allows it).
             * FIXME: The 'callF' method cannot notify the onDataReceived handlers!
             */
            Protocol.onDataReceived(requestTypeName, owner, response);
            return response;
        })
                : port;
    }

    /**
     * Submits multiple requests at once. It depends on the {@link Domain} of the receiver whether
     * the requests are dispatched synchronously, asynchronously, or in parallel.
     *
     * @return A {@link Fork} instance representing the asynchronous requests whose responses will be
     * received in the future.
     * @since 0.5.0
     */
    public Fork<O> fork(I... payloads) {
        return fork(Arrays.asList(payloads));
    }

    /**
     * Submits multiple requests at once. It depends on the {@link Domain} of the receiver whether
     * the requests are dispatched synchronously, asynchronously, or in parallel.
     *
     * <p> This method will call the given payload provider with integer arguments running from
     * 0 to 'endIndexExclusive' (exclusive, as the name suggests).
     *
     * @return A {@link Fork} instance representing the asynchronous requests whose responses will be
     * received in the future.
     * @since 0.5.0
     */
    public Fork<O> fork(int endIndexExclusive, IntFunction<I> payloadProvider) {
        return fork(IntStream.range(0, endIndexExclusive)
                .mapToObj(payloadProvider)
                .collect(Collectors.toList()));
    }

    /**
     * Submits multiple requests at once. It depends on the {@link Domain} of the receiver whether
     * the requests are dispatched synchronously, asynchronously, or in parallel.
     *
     * @return A {@link Fork} instance representing the asynchronous requests whose responses will be
     * received in the future.
     * @since 0.5.0
     */
    public Fork<O> fork(List<I> payloads) {
        Fork<O> fork = new Fork<>();

        for (I payload : payloads) {
            fork.add(callF(payload));
        }

        return fork;
    }

    /**
     * Returns true if this OUT port is connected to an IN port, false otherwise.
     */
    public boolean isConnected() {
        return port != null;
    }

    void clearCache() {
        cache.clear();
    }
}