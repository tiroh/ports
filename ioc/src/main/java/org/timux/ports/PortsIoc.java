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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

class PortsIoc {

    private static Map<String, ComponentInfo> components = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(PortsIoc.class);

    public static void register(Object component, Scope scope) {
        String componentName = component.getClass().getName();

        for (Map.Entry<String, ComponentInfo> e : components.entrySet()) {
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

        components.put(component.getClass().getName(), new ComponentInfo(component, scope));
        Ports.register(component);
    }

    private static void logConnection(Object from, String fromName, Object to, String toName, boolean isBidirectional) {
        String operator = isBidirectional ? "<->" : "->";

        logger.debug("Connected ports: {}:{} {} {}:{}",
                fromName, Integer.toHexString(from.hashCode()), operator, toName, Integer.toHexString(to.hashCode()));
    }

    public static String asString() {
        return components.toString();
    }
}
