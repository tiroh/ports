/*
 * Copyright 2018 Tim Rohlfs
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

/**
 * The main utility class of the Ports framework.
 *
 * @author Tim Rohlfs
 *
 * @since 0.1
 */
public final class Ports {

    private final static Map<Object, Field[]> fieldCache = new HashMap<>();
    private final static Map<Object, Method[]> methodCache = new HashMap<>();

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
        boolean portsWereConnected = false;

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

            ensurePortInstantiation(outPortField, from);

            Method inPortHandlerMethod = inPortHandlerMethodsByType.get(outPortFieldType);
            Field inPortField = inPortFieldsByType.get(outPortFieldType);

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
                    continue;
                }

                throw new PortNotFoundException(to.getClass().getName(), outPortFieldType);
            }

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
        }

        return portsWereConnected;
    }

    static void ensurePortInstantiation(Field outPortField, Object from) throws IllegalAccessException {
        if (outPortField.get(from) == null) {
            if (outPortField.getType() == Event.class) {
                String genericTypeName = outPortField.getGenericType().getTypeName();
                String extractedEventTypeName = extractTypeParameter(genericTypeName, genericTypeName);

                Event event = new Event(extractedEventTypeName, outPortField.getName(), from);
                outPortField.set(from, event);
            }

            if (outPortField.getType() == Request.class) {
                Request request = new Request(outPortField.getName(), from);
                outPortField.set(from, request);
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
                        event.disconnect(inPortField.get(to));
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

    private static Field[] getFields(Object o) {
        Field[] fields = fieldCache.get(o);

        if (fields == null) {
            fields = o.getClass().getDeclaredFields();

            for (Field field : fields) {
                field.setAccessible(true);
            }

            fieldCache.put(o, fields);
        }

        return fields;
    }

    private static Method[] getMethods(Object o) {
        Method[] methods = methodCache.get(o);

        if (methods == null) {
            methods = o.getClass().getDeclaredMethods();

            for (Method method : methods) {
                method.setAccessible(true);
            }

            methodCache.put(o, methods);
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
}
