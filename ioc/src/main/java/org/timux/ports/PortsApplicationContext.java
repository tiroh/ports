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

import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import org.timux.ports.types.Either;
import org.timux.ports.types.Failure;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortsApplicationContext {

    private final Map<String, ComponentInfo> classNamesToComponents = new HashMap<>();
    private final Map<String, ComponentInfo> portTypesToComponents = new HashMap<>();

    void scan(List<String> componentScanPackages) {
        ClasspathScanner scanner = new ClasspathScanner();

        scanner.scan(this, componentScanPackages).on(this::process, failure -> {
        });
    }

    void process(ClassInfoList classInfos) {
        register(classInfos);
    }

    void register(List<ClassInfo> classInfos) {
        classInfos.forEach(
                classInfo -> {
                    try {
                        Object instance = classInfo.loadClass().getConstructor().newInstance();
                        register(instance, Scope.SINGLETON);
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    void register(Object component, Scope scope) {
        String componentName = component.getClass().getName();

        for (Map.Entry<String, ComponentInfo> e : classNamesToComponents.entrySet()) {
            Object other = e.getValue().componentRef().get();

            if (other != null) {
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
        }

        classNamesToComponents.put(component.getClass().getName(), new ComponentInfo(component, scope));
        Ports.register(component);
    }

    private void logConnection(Object from, String fromName, Object to, String toName, boolean isBidirectional) {
        String operator = isBidirectional ? "<->" : "->";

        Logger.debug("Connected ports: {}:{} {} {}:{}",
                fromName, Integer.toHexString(from.hashCode()), operator, toName, Integer.toHexString(to.hashCode()));
    }

    String asString() {
        return classNamesToComponents.toString();
    }
}
