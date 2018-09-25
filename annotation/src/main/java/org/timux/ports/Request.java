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

import java.util.function.Function;

/**
 * A class that represents an OUT port with request-response semantics.
 *
 * @param <I> The type of the request data (input).
 * @param <O> The type of the response data (output).
 *
 * @author Tim Rohlfs
 *
 * @since 0.1
 */
public class Request<I, O> {

    private Function<I, O> port;
    private Object owner;
    private String name;

    /**
     * Connects this OUT port to the given IN port.
     *
     * @param port The IN port that this OUT port should be connected to.
     */
    public void connect(Function<I, O> port) {
        this.port = port;
    }

    /**
     * Sends the given payload to the connected IN port.
     *
     * @param payload The payload to be sent.
     *
     * @return The response of the connected component.
     */
    public O call(I payload) {
        if (port == null) {
            throw new PortNotConnectedException(name, owner.getClass().getName());
        }

        return port.apply(payload);
    }

    /**
     * Returns true if this OUT port is connected to an IN port, false otherwise.
     */
    public boolean isConnected() {
        return port != null;
    }
}