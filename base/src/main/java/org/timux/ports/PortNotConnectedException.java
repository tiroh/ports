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

import java.util.List;

public class PortNotConnectedException extends RuntimeException {

    PortNotConnectedException(String port, String component) {
        super(makeString(port, component));
    }

    public PortNotConnectedException(List<MissingPort> missingPorts) {
        super(makeStrings(missingPorts));
    }

    private static String makeStrings(List<MissingPort> missingPorts) {
        if (missingPorts.size() == 1) {
            return makeString(missingPorts.get(0).field.getName(), missingPorts.get(0).component.getClass().getName());
        }

        StringBuilder sb = new StringBuilder();

        missingPorts.forEach(p -> {
            sb.append("\n  ");
            sb.append(makeString(p.field.getName(), p.component.getClass().getName()));
        });

        return sb.toString();
    }

    private static String makeString(String port, String component) {
        return String.format("Port [%s] in %s is not connected.", port, component);
    }
}
