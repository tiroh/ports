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

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @since 0.5.0
 */
public class Domain {

    private final String name;
    private final SyncPolicy syncPolicy;
    private final DispatchPolicy dispatchPolicy;
    private final MessageQueue messageQueue;

    Domain(String name, SyncPolicy syncPolicy, DispatchPolicy dispatchPolicy) {
        this.name = name;
        this.syncPolicy = syncPolicy;
        this.dispatchPolicy = dispatchPolicy;

        switch (dispatchPolicy) {
        case SYNCHRONOUS:
            messageQueue = null;
            break;

        case ASYNCHRONOUS:
            messageQueue = new MessageQueue(name, 1);
            break;

        case PARALLEL:
            messageQueue = new MessageQueue(name, Runtime.getRuntime().availableProcessors());
            break;

        default:
            throw new IllegalStateException("unhandled dispatch policy: " + dispatchPolicy);
        }
    }

    public Domain addComponents(Object... components) {
        for (Object component : components) {
            DomainManager.register(component, this);
        }

        return this;
    }

    SyncPolicy getSyncPolicy() {
        return syncPolicy;
    }

    DispatchPolicy getDispatchPolicy() {
        return dispatchPolicy;
    }

    <T> void enqueue(Consumer<T> portFunction, T payload) {
        messageQueue.enqueue(portFunction, payload);
    }

    <I, O> PortsFuture<O> enqueue(Function<I, O> portFunction, I payload) {
        return messageQueue.enqueue(portFunction, payload);
    }

    void awaitQuiescence() {
        if (messageQueue != null) {
            messageQueue.awaitQuiescence();
        }
    }

    @Override
    public String toString() {
        return "Domain{'" + name + "', " + syncPolicy + ", " + dispatchPolicy + "}";
    }
}
