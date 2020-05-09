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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
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

    private static final Map<Class<?>, Field[]> fieldCache = new HashMap<>();
    private static final Map<Class<?>, Method[]> methodCache = new HashMap<>();

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
            if (inPortField.getType() == Queue.class) {
                if (inPortField.get(to) == null) {
                    inPortField.set(to, new Queue());
                }
            }

            if (inPortField.getType() == Stack.class) {
                if (inPortField.get(to) == null) {
                    inPortField.set(to, new Stack());
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
                    if (inPortField.getType() == Queue.class) {
                        event.connect((Queue) inPortField.get(to));
                        portsWereConnected = true;
                    }

                    if (inPortField.getType() == Stack.class) {
                        event.connect((Stack) inPortField.get(to));
                        portsWereConnected = true;
                    }
                }
            }
        }

        if (outPortField.getType() == Request.class) {
            Request request = (Request) outPortField.get(from);

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
            String extractedMessageTypeName = extractTypeParameter(genericTypeName, genericTypeName);
            String requestTypeName = extractRequestTypeName(extractedMessageTypeName);

            if (outPortField.getType() == Event.class) {
                Event event = new Event(requestTypeName, owner);
                outPortField.set(owner, event);
            }

            if (outPortField.getType() == Request.class) {
                Request request = new Request(requestTypeName, outPortField.getName(), owner);
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

            String typeParameter = extractTypeParameter(field.getGenericType().getTypeName(), "");

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
        Field[] fields = fieldCache.get(clazz);

        if (fields == null) {
            fields = clazz.getDeclaredFields();

            for (Field field : fields) {
                field.setAccessible(true);
            }

            fieldCache.put(clazz, fields);
        }

        return fields;
    }

    private synchronized static Method[] getMethods(Object o) {
        Class<?> clazz = o.getClass();
        Method[] methods = methodCache.get(clazz);

        if (methods == null) {
            methods = clazz.getDeclaredMethods();

            for (Method method : methods) {
                method.setAccessible(true);
            }

            methodCache.put(clazz, methods);
        }

        return methods;
    }

    private static String extractTypeParameter(String type, String _default) {
        int genericStart = type.indexOf('<');
        int genericEnd = type.lastIndexOf('>');

        return genericStart < 0
                ? _default
                : type.substring(genericStart + 1, genericEnd);
    }

    private static String extractRequestTypeName(String type) {
        return type.split(",")[0].trim();
    }

    private static String extractResponseTypeName(String type) {
        try {
            return type.split(",")[1].trim();
        } catch (IndexOutOfBoundsException e) {
            return void.class.getName();
        }
    }

    /**
     * Registers the given components for use in protocols. This is only necessary if the 'with' syntax without
     * explicitly provided port owner shall be used.
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
     * Begins declaration of a new protocol. When using this when writing unit tests, remember to call
     * {@link #releaseProtocols()} before each individual test so that each test starts with a clean
     * protocol setup.
     *
     * @since 0.5.0
     */
    public static ConditionOrAction<?> protocol() {
        Protocol.areProtocolsActive = true;
        return new ConditionOrAction<>(new ProtocolParserState());
    }

    /**
     * Releases all previously declared protocols. When writing unit tests with protocols, this must be called
     * before each individual test method.
     *
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
        MessageQueue.awaitQuiescence();
    }

    /**
     * Specifies how Ports should handle asynchronicity.
     *
     * @since 0.5.0
     */
    public static void setAsyncPolicy(AsyncPolicy asyncPolicy) {
        MessageQueue.setAsyncPolicy(asyncPolicy);
    }

    static void printWarning(String message) {
        System.err.println("[ports] warning: " + message);
    }
}
