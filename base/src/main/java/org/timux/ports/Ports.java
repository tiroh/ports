/*
 * Copyright 2018-2022 Tim Rohlfs
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
import org.timux.ports.types.Nothing;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * The main utility class of the Ports Framework.
 *
 * @author Tim Rohlfs
 *
 * @since 0.1
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class Ports {

    private static final Map<Class<?>, WeakReference<Field[]>> fieldCache = new WeakHashMap<>();
    private static final Map<Class<?>, WeakReference<Method[]>> methodCache = new WeakHashMap<>();

    private static final PortsEventExceptionSender eventExceptionSender = new PortsEventExceptionSender();

    /* This map exists so that the user can register arbitrary data with Ports.
     * This is useful when Spring Boot live-reloads Java classes and destroys their
     * state (including static state). In the default scenario, the classes in the Ports
     * packages will not be reloaded so that their state is preserved. */
    private static final Map<String, Object> customDataRegistry = new HashMap<>();

    private Ports() {
        // Don't you instantiate this class!!
    }

    /**
     * Begins connecting two components using the style of a fluent API.
     *
     * @param a The first component of the connection.
     *
     * @return An object that enables specifying the second component of the connection.
     */
    public static AndClause connect(Object a) {
        if (a == null) {
            throw new IllegalArgumentException("component must not be null");
        }

        return new AndClause((b, portsOptions) -> connectBoth(a, b, portsOptions));
    }

    /**
     * Begins disconnecting two components using the style of a fluent API.
     *
     * @param a The first component of the connection.
     *
     * @return An object that enables specifying the second component of the connection.
     */
    public static AndClause disconnect(Object a) {
        if (a == null) {
            throw new IllegalArgumentException("component must not be null");
        }

        return new AndClause((b, portsOptions) -> disconnectBoth(a, b, portsOptions));
    }

    /**
     * Disconnects all of the specified components from each other.
     */
    public static void disconnect(Object... components) {
        for (int i = 0; i < components.length; i++) {
            for (int j = i  + 1; j < components.length; j++) {
                disconnect(components[i]).and(components[j]);
            }
        }
    }

    static boolean connectBoth(Object a, Object b, int portsOptions) {
        try {
            boolean s = connectDirectedInternal(a, b, portsOptions);
            boolean t = connectDirectedInternal(b, a, portsOptions);

            return s || t;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean connectDirected(Object from, Object to, int portsOptions) {
        return connectDirected(from, to, null, portsOptions);
    }

    public static boolean connectDirected(Object from, Object to, EventWrapper eventWrapper, int portsOptions) {
        try {
            return connectDirectedInternal(from, to, eventWrapper, portsOptions);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static boolean connectDirectedInternal(Object from, Object to, int portsOptions) throws IllegalAccessException {
        return connectDirectedInternal(from, to, null, portsOptions);
    }

    static boolean connectDirectedInternal(Object from, Object to, EventWrapper eventWrapper, int portsOptions) throws IllegalAccessException {
        if (from != eventExceptionSender) {
            connectDirectedInternal(eventExceptionSender, to, PortsOptions.DEFAULT);
        }

        Map<String, Method> inPortHandlerMethodsByType = getInPortHandlerMethodsByType(from, to);

        Map<String, Field> outPortFieldsByType;
        Map<String, Field> inPortFieldsByType;

        try {
            outPortFieldsByType = getPortFieldsByType(from, Out.class, false);
            inPortFieldsByType = getPortFieldsByType(to, In.class, false);
        } catch (DuplicateTypesException e) {
            throw new AmbiguousPortsException(from.getClass().getName(), to.getClass().getName(), e.getMessage());
        }

        boolean portsWereConnected = false;

        for (Map.Entry<String, Field> e : outPortFieldsByType.entrySet()) {
            String outPortFieldType = e.getKey();
            Field outPortField = e.getValue();

            ensurePortInstantiation(outPortField, from);

            Method inPortHandlerMethod = inPortHandlerMethodsByType.get(outPortFieldType);
            Field inPortField = inPortFieldsByType.get(outPortFieldType);

            portsWereConnected |= connectSinglePort(
                    outPortField,
                    outPortFieldType,
                    from,
                    to,
                    inPortHandlerMethod,
                    inPortField,
                    eventWrapper,
                    portsOptions);
        }

        return portsWereConnected;
    }

    static boolean connectSinglePort(
            Field outPortField,
            String outPortFieldType,
            Object from,
            Object to,
            Method inPortHandlerMethod,
            Field inPortField,
            EventWrapper eventWrapper,
            int portsOptions) throws IllegalAccessException
    {
        if (inPortField != null) {
            if (inPortField.getType() == QueuePort.class) {
                if (inPortField.get(to) == null) {
                    inPortField.set(to, new QueuePort());
                }
            }

            if (inPortField.getType() == StackPort.class) {
                if (inPortField.get(to) == null) {
                    inPortField.set(to, new StackPort());
                }
            }
        }

        if (inPortHandlerMethod != null && inPortField != null) {
            throw new AmbiguousPortsException(from.getClass().getName(), to.getClass().getName(), outPortFieldType);
        }

        if (inPortHandlerMethod == null && inPortField == null) {
            if ((portsOptions & PortsOptions.DO_NOT_ALLOW_MISSING_PORTS) == 0) {
                return false;
            }

            throw new PortNotFoundException(outPortFieldType, to.getClass().getName());
        }

        boolean portsWereConnected = false;

        if (outPortField.getType() == Event.class) {
            Event event = (Event) outPortField.get(from);

            if (!event.isConnected()
                    || ((portsOptions & PortsOptions.FORCE_CONNECT_ALL) != 0)
                    || ((portsOptions & PortsOptions.FORCE_CONNECT_EVENT_PORTS) != 0))
            {
                if (inPortHandlerMethod != null) {
                    event.connect(inPortHandlerMethod, to, eventWrapper);
                    portsWereConnected = true;
                }

                if (inPortField != null) {
                    if (inPortField.getType() == QueuePort.class) {
                        event.connect((QueuePort) inPortField.get(to), to);
                        portsWereConnected = true;
                    }

                    if (inPortField.getType() == StackPort.class) {
                        event.connect((StackPort) inPortField.get(to), to);
                        portsWereConnected = true;
                    }
                }
            }
        }

        if (outPortField.getType() == Request.class) {
            Request request = (Request) outPortField.get(from);

            if (request.isConnected() && (portsOptions & PortsOptions.FAIL_ON_AMBIGUOUS_REQUEST_CONNECTIONS) != 0) {
                throw new AmbiguousRequestConnectionException(request.getRequestTypeName(), from.getClass().getName(), to.getClass().getName());
            }

            if (!request.isConnected() || ((portsOptions & PortsOptions.FORCE_CONNECT_ALL) != 0)) {
                request.connect(inPortHandlerMethod, to);
                portsWereConnected = true;
            }
        }

        return portsWereConnected;
    }

    static boolean connectSinglePort(
            Field outPortField,
            String outPortFieldType,
            Object from,
            Object to,
            int portsOptions) throws IllegalAccessException
    {
        Map<String, Method> inPortHandlerMethodsByType = getInPortHandlerMethodsByType(from, to);

        Map<String, Field> inPortFieldsByType;

        try {
            inPortFieldsByType = getPortFieldsByType(to, In.class, false);
        } catch (DuplicateTypesException e) {
            throw new AmbiguousPortsException(from.getClass().getName(), to.getClass().getName(), e.getMessage());
        }

        Method inPortHandlerMethod = inPortHandlerMethodsByType.get(outPortFieldType);
        Field inPortField = inPortFieldsByType.get(outPortFieldType);

        return connectSinglePort(
                outPortField,
                outPortFieldType,
                from,
                to,
                inPortHandlerMethod,
                inPortField,
                null,
                portsOptions);
    }

    static void ensurePortInstantiation(Field outPortField, Object owner) throws IllegalAccessException {
        if (outPortField.get(owner) == null) {
            String genericTypeName = outPortField.getGenericType().getTypeName();
            String extractedMessageTypeName = TypeUtils.extractTypeParameter(genericTypeName, genericTypeName);
            String requestTypeName = TypeUtils.extractRequestTypeName(extractedMessageTypeName);

            if (outPortField.getType() == Event.class) {
                Event event = new Event(requestTypeName, owner);
                outPortField.set(owner, event);
            }

            if (outPortField.getType() == Request.class) {
                Request request = new Request(requestTypeName, outPortField, outPortField.getName(), owner);
                outPortField.set(owner, request);
            }
        }
    }

    static void disconnectBoth(Object a, Object b, int portsOptions) {
        disconnectDirected(a, b, portsOptions);
        disconnectDirected(b, a, portsOptions);
    }

    static void disconnectDirected(Object from, Object to, int portsOptions) {
        Map<String, Method> inPortHandlerMethodsByType = getInPortHandlerMethodsByType(from, to);

        Map<String, Field> outPortFieldsByType;
        Map<String, Field> inPortFieldsByType;

        try {
            outPortFieldsByType = getPortFieldsByType(from, Out.class, false);
            inPortFieldsByType = getPortFieldsByType(to, In.class, false);
        } catch (DuplicateTypesException e) {
            throw new AmbiguousPortsException(from.getClass().getName(), to.getClass().getName(), e.getMessage());
        }

        for (Map.Entry<String, Field> e : outPortFieldsByType.entrySet()) {
            String outPortFieldType = e.getKey();
            Field outPortField = e.getValue();

            Method inPortHandlerMethod = inPortHandlerMethodsByType.get(outPortFieldType);
            Field inPortField = inPortFieldsByType.get(outPortFieldType);

            try {
                if (outPortField.getType() == Event.class) {
                    Event event = (Event) outPortField.get(from);

                    if (inPortHandlerMethod != null) {
                        event.disconnect(inPortHandlerMethod, to);
                    }

                    if (inPortField != null) {
                        event.disconnect((Consumer) inPortField.get(to));
                    }
                }

                if (outPortField.getType() == Request.class) {
                    if (inPortHandlerMethod != null) {
                        Request request = (Request) outPortField.get(from);
                        request.disconnect();
                    }
                }
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static Map<String, Field> getPortFieldsByType(Object component, Class annotationType, boolean allowDuplicateTypes) {
        Map<String, Field> fieldsByType = new HashMap<>();
        Field[] fromFields = getFields(component);

        for (Field field : fromFields) {
            if (field.getAnnotation(annotationType) == null) {
                continue;
            }

            String typeParameter = TypeUtils.extractTypeParameter(field.getGenericType().getTypeName(), "");

            if (typeParameter.isEmpty()) {
                typeParameter = (field.getType() == Request.class ? "java.lang.Object, java.lang.Object" : "java.lang.Object");
            }

            if (field.getType() != Request.class) {
                typeParameter += ", void";
            }

            if (!allowDuplicateTypes && fieldsByType.containsKey(typeParameter)) {
                throw new DuplicateTypesException(typeParameter);
            }

            fieldsByType.put(typeParameter, field);
        }

        return fieldsByType;
    }

    private static Map<String, Method> getInPortHandlerMethodsByType(Object from, Object to) {
        Map<String, Method> methodsByType = new HashMap<>();
        Method[] toMethods = getMethods(to);

        for (Method method : toMethods) {
            if (method.getAnnotation(In.class) == null) {
                continue;
            }

            String typeString = Arrays.stream(method.getGenericParameterTypes())
                    .map(Type::getTypeName)
                    .reduce((r, x) -> r + "," + x)
                    .orElse("-")
                    + ", " + method.getGenericReturnType().getTypeName();

            if (methodsByType.containsKey(typeString)) {
                throw new AmbiguousPortsException(from.getClass().getName(), to.getClass().getName(), typeString);
            }

            methodsByType.put(typeString, method);
        }

        return methodsByType;
    }

    /**
     * Checks whether all OUT ports of the provided components are connected.
     *
     * @throws PortNotConnectedException If there is an OUT port that is not connected.
     *
     * @param components The components to check.
     */
    public static void verify(Object... components) {
        List<MissingPort> missingPorts = verifyInternal(true, components);

        if (!missingPorts.isEmpty()) {
            throw new PortNotConnectedException(missingPorts);
        }
    }

    /**
     * Checks whether all {@link Request} ports of the provided components are connected.
     *
     * @throws PortNotConnectedException If there is a Request port that is not connected.
     *
     * @param components The components to check.
     */
    public static void verifyRequests(Object... components) {
        List<MissingPort> missingPorts = verifyInternal(false, components);

        if (!missingPorts.isEmpty()) {
            throw new PortNotConnectedException(missingPorts);
        }
    }

    /*
     * This method must not change its name or its signature, it is being called via
     * reflection by PortConnector (vaadinspring module).
     */
    static List<MissingPort> verifyInternal(boolean alsoVerifyEventPorts, Object... components) {
        List<MissingPort> missingPorts = new ArrayList<>();

        try {
            for (Object component : components) {
                Field[] fields = getFields(component);

                for (Field field : fields) {
                    if (field.getAnnotation(Out.class) == null) {
                        continue;
                    }

                    if (field.getType() == Event.class && alsoVerifyEventPorts) {
                        Event event = (Event) field.get(component);

                        if (event == null || !event.isConnected()) {
                            missingPorts.add(new MissingPort(field, component));
                        }
                    }

                    if (field.getType() == Request.class) {
                        Request request = (Request) field.get(component);

                        if (request == null || !request.isConnected()) {
                            missingPorts.add(new MissingPort(field, component));
                        }
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return missingPorts;
    }

    private synchronized static Field[] getFields(Object o) {
        Class<?> clazz = o.getClass();
        WeakReference<Field[]> fieldsRef = fieldCache.get(clazz);
        Field[] fields = fieldsRef != null ? fieldsRef.get() : null;

        if (fields == null) {
            fields = clazz.getDeclaredFields();

            for (Field field : fields) {
                field.setAccessible(true);
            }

            fieldCache.put(clazz, new WeakReference<>(fields));
        }

        return fields;
    }

    private synchronized static Method[] getMethods(Object o) {
        Class<?> clazz = o.getClass();
        WeakReference<Method[]> methodsRef = methodCache.get(clazz);
        Method[] methods = methodsRef != null ?  methodsRef.get() : null;

        if (methods == null) {
            methods = clazz.getDeclaredMethods();

            for (Method method : methods) {
                method.setAccessible(true);
            }

            methodCache.put(clazz, new WeakReference<>(methods));
        }

        return methods;
    }

    /**
     * Registers the provided components for use in protocols. This is only necessary if the 'with' syntax
     * without explicitly provided port owner shall be used.
     *
     * <p>Note that you are responsible for taking care that the components are not garbage-collected
     * while you still need them. The protocols will not prevent them from being GC'd.
     *
     * @since 0.5.0
     */
    public static void register(Object... components) {
        for (Object component : components) {
            Map<String, Field> outPortFieldsByType = getPortFieldsByType(component, Out.class, true);

            try {
                for (Map.Entry<String, Field> e : outPortFieldsByType.entrySet()) {
                    Field outPortField = e.getValue();
                    ensurePortInstantiation(outPortField, component);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            Protocol.registerComponent(component);
        }
    }

    /**
     * Unregisters the provided components from the protocols.
     *
     * @since 0.7.0
     */
    public static void unregister(Object... components) {
        for (Object component : components) {
            Protocol.unregisterComponent(component);
        }
    }

    /**
     * Begins declaration of a new protocol with the empty string as protocol identifier.
     * See {@link #protocol(String)} for details.
     *
     * @since 0.5.0
     */
    public static ConditionOrAction<?> protocol() {
        return protocol("");
    }

  /**
   * Begins declaration of a new protocol identified by the provided protocol identifier. When using
   * this in tests, remember to call {@link #releaseProtocols()} before each individual test so that
   * each test starts with a clean protocol setup.
   *
   * <p>The protocol identifier can be used later to release individual protocols from the registry.
   * See {@link #releaseProtocol(String)} for details.
   *
   * <p>It is admissible to make multiple calls to this method using the same protocol identifier.
   * In this case, the named protocol will be augmented with the specified actions.
   *
   * @since 0.7.0
   */
  public static ConditionOrAction<?> protocol(String protocolIdentifier) {
        Protocol.areProtocolsActive = true;
        DomainManager.invalidate();
        return new ConditionOrAction<>(new ProtocolParserState(protocolIdentifier));
    }

    /**
     * Releases the protocol with the provided identifier from the registry.
     *
     * @see #releaseProtocols()
     * @since 0.7.0
     */
    public static void releaseProtocol(String protocolIdentifier) {
        Protocol.release(protocolIdentifier);
    }

    /**
     * Releases all previously declared protocols. When writing unit tests with protocols, this must be called
     * after each individual test method so that subsequent tests are not influenced by the preceding tests'
     * protocols.
     *
     * <p>When using JUnit, it is recommended to create an 'afterEach' method calling this method.
     * 
     * @see #releaseProtocol(String)
     * @since 0.5.0
     */
    public static void releaseProtocols() {
        Protocol.clear();
    }

    /**
     * Waits until all messaging threads are quiescent. This method is normally used in testing
     * when messages are sent asynchronously. In this case, it can happen that at that point in time when
     * test assertions are made not all threads have finished working. So, before any assertions can be
     * made, this method has to be called in order to ensure that no threads are active anymore.
     *
     * @since 0.5.0
     */
    public static void awaitQuiescence() {
        DomainManager.awaitQuiescence();
    }

    /**
     * Configures a synchronization domain. Each Ports component is assigned to exactly one
     * synchronization domain that specifies how (a) messages are dispatched (synchronously,
     * asynchronously, or in parallel) and (b) how parallel accesses are synchronized.
     *
     * <p> You should configure synchronization domains as early as possible during application
     * startup so that all Ports communication is handled correctly.
     *
     * <p> By default, each component is assigned to a default domain that dispatches synchronously
     * ({@link DispatchPolicy#SYNCHRONOUS}) and that synchronizes on component level
     * ({@link SyncPolicy#COMPONENT}).
     *
     * @see SyncPolicy
     * @see DispatchPolicy
     *
     * @since 0.5.0
     */
    public static Domain domain(String name, DispatchPolicy dispatchPolicy, SyncPolicy syncPolicy) {
        return new Domain(name, dispatchPolicy, syncPolicy);
    }

    /**
     * Removes all synchronization domains from the registry. This causes all components to fall
     * back to the default domain (which uses synchronous dispatch and component-level synchronization).
     *
     * <p> This method instructs all threads that might have been created by the domains to shutdown.
     * They will not shutdown immediately but try to finish their current tasks. Any further queued messages
     * will not be processed.
     *
     * <p> This method is ONLY meant to be used in testing in a '@BeforeEach' method (JUnit). You should
     * not use it in production since abandoning the message queues can leave the system in an undefined
     * state.
     *
     * @since 0.5.0
     */
    public static void releaseDomains() {
        DomainManager.release();
    }

    /**
     * Clears the caches of all ports.
     *
     * @since 0.6.0
     */
    public static void clearCaches() {
        CacheManager.clear();
    }

    /**
     * Resets all internal state information, i.e. protocols and domains, and clears all caches.
     *
     * @since 0.6.0
     */
    public static void reset() {
        releaseProtocols();
        releaseDomains();
        CacheManager.reset();
        eventExceptionSender.disconnect();
        clearCustomData();
    }

    /**
     * Puts the provided key/value pair into the custom data registry.
     * This is useful when a permanent static storage is required, for example
     * when Spring Boot live-reloads the application's Java classes and
     * destroys their static state.
     *
     * @since 0.7.0
     */
    public static void putCustomData(String key, Object value) {
        synchronized (customDataRegistry) {
            customDataRegistry.put(key, value);
        }
    }

    /**
     * Returns the custom data registry value associated with the provided key or
     * {@link Nothing}, if it doesn't exist.
     *
     * @since 0.7.0
     */
    public static Either<Object, Nothing> getCustomData(String key) {
        synchronized (customDataRegistry) {
            return Either.ofNullable(customDataRegistry.get(key));
        }
    }

    /**
     * Returns true if the custom data registry contains the provided key,
     * or false otherwise.
     *
     * @since 0.7.0
     */
    public static boolean containsCustomData(String key) {
        synchronized (customDataRegistry) {
            return customDataRegistry.containsKey(key);
        }
    }

    /**
     * Removes the provided key from the custom data registry.
     *
     * @return True if the registry contained the provided key, false otherwise.
     *
     * @since 0.7.0
     */
    public static boolean removeCustomData(String key) {
        synchronized (customDataRegistry) {
            return customDataRegistry.remove(key) != null;
        }
    }

    /**
     * Clears the custom data registry.
     *
     * @since 0.7.0
     */
    public static void clearCustomData() {
        synchronized (customDataRegistry) {
            customDataRegistry.clear();
        }
    }

    static void printWarning(String message) {
        System.err.println("[ports] warning: " + message);
    }

    static void printError(String message) {
        System.err.println("[ports] error: " + message);
    }

    static void triggerEventException(Throwable throwable) {
        eventExceptionSender.trigger(throwable);
    }

    public static String getVersionString() {
        Properties properties = new Properties();

        try {
            properties.load(Ports.class.getClassLoader().getResourceAsStream("ports.properties"));
            return properties.getProperty("version", "?");
        } catch (IOException e) {
            e.printStackTrace();
            return "?";
        }
    }
}
