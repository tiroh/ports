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
 
package org.timux.ports.verification;

class PortNamer {

    public static String toOutPortName(String messageType) {
        String suffix = messageType.substring(messageType.lastIndexOf('.') + 1)
                + (messageType.endsWith("Exception") ? "Event" : "");

        return Character.toLowerCase(suffix.charAt(0)) + suffix.substring(1);
    }

    public static String toInPortName(String messageType) {
        String suffix = messageType.substring(messageType.lastIndexOf('.') + 1);

        if (messageType.endsWith("Event")) {
            return "on" + suffix.substring(0, suffix.length() - "Event".length());
        } else {
            return "on" + suffix;
        }
    }
}
