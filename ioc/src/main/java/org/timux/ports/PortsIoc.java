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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

class PortsIoc {

    private static final Map<String, ComponentInfo> components = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(PortsIoc.class);

    synchronized static void registerStatic(Object component) {
        for (Map.Entry<String, ComponentInfo> e : components.entrySet()) {
            e.getValue().getComponents().on(
                    staticComponent -> {
                        connect(component, staticComponent);
                    },
                    dynamicComponentRefs -> {
                        dynamicComponentRefs.keySet().forEach(other -> {
                            connect(component, other);
                        });
                    });
        }

        components.put(component.getClass().getName(), ComponentInfo.ofStatic(component));
    }

    synchronized static void registerDynamic(Object component) {
        ComponentInfo componentInfo = components.get(component.getClass().getName());

        if (componentInfo == null) {
            componentInfo = ComponentInfo.ofDynamic(component);
            components.put(component.getClass().getName(), componentInfo);
        } else {
            componentInfo.getComponents().onB(refs -> refs.put(new WeakReference<>(component), null));
        }

        for (Map.Entry<String, ComponentInfo> e : components.entrySet()) {
            e.getValue().getComponents().on(
                    staticComponent -> {
                        if (staticComponent != component) {
                            connect(component, staticComponent);
                        }
                    },
                    dynamicComponentRefs -> {
                        dynamicComponentRefs.keySet().forEach(dynamicComponent -> {
                            if (dynamicComponent != component) {
                                connect(component, dynamicComponent);
                            }
                        });
                    });
        }
    }

    private synchronized static void connect(Object component, Object other) {
        String componentName = component.getClass().getName();
        String otherName = other.getClass().getName();

        boolean a = Ports.connectDirected(component, other, PortsOptions.FORCE_CONNECT_EVENT_PORTS);
        boolean b = Ports.connectDirected(other, component, PortsOptions.FORCE_CONNECT_EVENT_PORTS);

        if (a && b) {
            logConnection(component, componentName, other, otherName, true);
        } else if (a) {
            logConnection(component, componentName, other, otherName, false);
        } else if (b) {
            logConnection(other, otherName, component, componentName, false);
        }
    }

    public synchronized static <T> T getOrMakeInstance(Class<T> componentClass) {
        ComponentInfo componentInfo = components.get(componentClass.getName());

        if (componentInfo != null) {
            Object component = componentInfo.getComponents().map(Function.identity(), refs -> null);

            if (component != null) {
                return (T) component;
            }
        }

        Static staticAnno = componentClass.getAnnotation(Static.class);

        if (staticAnno != null) {
            try {
                T component = componentClass.getConstructor().newInstance();
                registerStatic(component);
                return component;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Dynamic dynamicAnno = componentClass.getAnnotation(Dynamic.class);

        if (dynamicAnno == null) {
            throw new IllegalArgumentException("class must be marked '@Dynamic' in order to be dynamically instantiable: "
                    + componentClass.getName());
        }

        try {
            T component = componentClass.getConstructor().newInstance();
            registerDynamic(component);
            return component;
        } catch (Exception e) {
            e.printStackTrace();
        }

        throw new IllegalArgumentException("class must be marked either '@Static' or '@Dynamic' in order to be "
                + "eligible for IoC instantiation");
    }

    synchronized static void instantiateStaticComponents() {
        ClasspathScanner.getStatics().values().forEach(
                clazz -> {
                    try {
                        Object instance = clazz.getConstructor().newInstance();
                        registerStatic(instance);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    private static void logConnection(Object from, String fromName, Object to, String toName, boolean isBidirectional) {
        String operator = isBidirectional ? "<->" : "->";

        logger.debug("Connected ports: {}:{} {} {}:{}",
                fromName, Integer.toHexString(from.hashCode()), operator, toName, Integer.toHexString(to.hashCode()));
    }

    synchronized static void clear() {
        components.clear();
    }
}
