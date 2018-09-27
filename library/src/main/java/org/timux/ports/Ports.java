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

import org.timux.ports.agent.TransformingVisitor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The main utility class of the Ports framework.
 *
 * @author Tim Rohlfs
 *
 * @since 0.1
 */
public final class Ports {

    private static Map<Object, Field[]> fieldCache = new HashMap<>();

    private static Map<Object, Method[]> methodCache = new HashMap<>();

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

    static void connectBoth(Object a, Object b, int portsOptions) {
        connectDirected(a, b, portsOptions);
        connectDirected(b, a, portsOptions);
    }

    static void connectDirected(Object from, Object to, int portsOptions) {
        Map<String, Method> inPortHandlerMethodsByType = getInPortHandlerMethodsByType(from, to);
        Map<String, Field> inPortHandlerFieldsByName = getInPortHandlerFieldsByName(to);

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

            if (inPortHandlerMethod != null && inPortField != null) {
                throw new AmbiguousPortsException(from.getClass().getName(), to.getClass().getName(), outPortFieldType);
            }

            if (inPortHandlerMethod == null && inPortField == null) {
                if ((portsOptions & PortsOptions.DO_NOT_ALLOW_MISSING_PORTS) == 0) {
                    continue;
                }

                throw new PortNotFoundException(to.getClass().getName(), outPortFieldType);
            }

            try {
                if (outPortField.getType() == Event.class) {
                    Event event = (Event) outPortField.get(from);

                    if (!event.isConnected() || ((portsOptions & PortsOptions.FORCE_CONNECT_ALL) != 0)) {
                        if (inPortHandlerMethod != null) {
                            Field inPortHandlerField = inPortHandlerFieldsByName.get(inPortHandlerMethod.getName());
                            event.connect((Consumer) inPortHandlerField.get(to));
                        }

                        if (inPortField != null) {
                            if (inPortField.getType() == Queue.class) {
                                event.connect((Queue) inPortField.get(to));
                            }

                            if (inPortField.getType() == Stack.class) {
                                event.connect((Stack) inPortField.get(to));
                            }
                        }
                    }
                }

                if (outPortField.getType() == Request.class) {
                    Request request = (Request) outPortField.get(from);

                    if (!request.isConnected() || ((portsOptions & PortsOptions.FORCE_CONNECT_ALL) != 0)) {
                        Field inPortHandlerField = inPortHandlerFieldsByName.get(inPortHandlerMethod.getName());
                        request.connect((Function) inPortHandlerField.get(to));
                    }
                }
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    static void disconnectBoth(Object a, Object b, int portsOptions) {
        disconnectDirected(a, b, portsOptions);
        disconnectDirected(b, a, portsOptions);
    }

    static void disconnectDirected(Object from, Object to, int portsOptions) {
        Map<String, Method> inPortHandlerMethodsByType = getInPortHandlerMethodsByType(from, to);
        Map<String, Field> inPortHandlerFieldsByName = getInPortHandlerFieldsByName(to);

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
                        Field inPortHandlerField = inPortHandlerFieldsByName.get(inPortHandlerMethod.getName());
                        event.disconnect(inPortHandlerField.get(to));
                    }

                    if (inPortField != null) {
                        event.disconnect(inPortField.get(to));
                    }
                }

                if (outPortField.getType() == Request.class) {
                    Request request = (Request) outPortField.get(from);
                    request.disconnect();
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

            String genType = field.getGenericType().getTypeName();

            String paramType = genType.indexOf('<') == -1
                    ? (field.getType() == Request.class ? "java.lang.Object, java.lang.Object" : "java.lang.Object")
                    : genType.substring(genType.indexOf('<') + 1, genType.lastIndexOf('>'));

            if (field.getType() != Request.class) {
                paramType += ", void";
            }

            if (!allowDuplicateTypes && fieldsByType.containsKey(paramType)) {
                throw new DuplicateTypesException(paramType);
            }

            fieldsByType.put(paramType, field);
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
                    .map(x -> x.getTypeName())
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

    private static Map<String, Field> getInPortHandlerFieldsByName(Object to) {
        Map<String, Field> handlersByName = new HashMap<>();
        Field[] toFields = getFields(to);
        String flimflam = "";

        for (Field field : toFields) {
            final String fieldName = field.getName();

            if (fieldName.startsWith(TransformingVisitor.HANDLER_PREFIX) && fieldName.endsWith(flimflam)) {
                handlersByName.put(fieldName.substring(TransformingVisitor.HANDLER_PREFIX.length(), fieldName.lastIndexOf('_')), field);

                if (flimflam.isEmpty()) {
                    flimflam = fieldName.substring(fieldName.length() - 64 / 4);
                }
            }
        }

        return handlersByName;
    }

    /**
     * Checks whether all OUT ports of the provided components are connected.
     *
     * @throws PortNotConnectedException If there is an OUT port that is not connected.
     *
     * @param components The components to check.
     */
    public static void verify(Object... components) {
        try {
            for (Object component : components) {
                Field[] fields = getFields(component);

                for (Field field : fields) {
                    if (field.getAnnotation(Out.class) == null) {
                        continue;
                    }

                    if (field.getType() == Event.class) {
                        Event event = (Event) field.get(component);

                        if (!event.isConnected()) {
                            throw new PortNotConnectedException(field.getName(), component.getClass().getName());
                        }
                    }

                    if (field.getType() == Request.class) {
                        Request request = (Request) field.get(component);

                        if (!request.isConnected()) {
                            throw new PortNotConnectedException(field.getName(), component.getClass().getName());
                        }
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
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
}
