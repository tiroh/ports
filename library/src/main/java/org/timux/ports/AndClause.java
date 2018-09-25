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

/**
 * A class that allows specifying the second component of a connection.
 *
 * @author Tim Rohlfs
 *
 * @since 0.1
 */
public class AndClause {

    private Object a;

    protected AndClause(Object a) {
        this.a = a;
    }

    /**
     * Connects the given component with the component that has been configured earlier. Default options are used.
     *
     * @param b The second component of the connection.
     */
    public void and(Object b) {
        Ports.connectBoth(a, b);
    }

    /**
     * Connects the given component with the component that has been configured earlier.
     *
     * @param b The second component of the connection.
     * @param portsOptions A bit field specifying a set of {@link PortsOptions}.
     */
    public void and(Object b, int portsOptions) {
        Ports.connectBoth(a, b, portsOptions);
    }
}
