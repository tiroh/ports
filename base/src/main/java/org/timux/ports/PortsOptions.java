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
 * Defines options that are used to control the behavior of Ports connections.
 *
 * @author Tim Rohlfs
 *
 * @since 0.1
 */
public class PortsOptions {

    /* Note that we use Integers instead of ints because there are cases were these constants
     * are used as arguments to generic functions. Using an int would result in boxing the
     * values again and again. */

    /**
     * The default behavior is to connect only unconnected ports and to fail with an exception
     * if a port cannot be connected due to a missing handler.
     */
    public static final Integer DEFAULT = 0;

    /**
     * Connect all ports, even those that are already connected. (By default, ports that are already
     * connected would not be reconnected.)
     */
    public static final Integer FORCE_CONNECT_ALL = 1;

    /**
     * Connect all event ports, even those that are already connected. Also connect Request ports,
     * but do not reconnect those.
     */
    public static final Integer FORCE_CONNECT_EVENT_PORTS = 2;

    /**
     * If an OUT port cannot be connected because of a missing IN port, fail with an exception.
     */
    public static final Integer DO_NOT_ALLOW_MISSING_PORTS = 4;

    /**
     * If a request port is already connected but a further connection candidate is encountered,
     * fail with an exception.
     */
    public static final Integer FAIL_ON_AMBIGUOUS_REQUEST_CONNECTIONS = 8;
}
