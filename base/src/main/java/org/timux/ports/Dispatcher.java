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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("rawtypes")
class Dispatcher {

    private final Deque<Task> regularQueue = new ArrayDeque<>();
    private final Executor workerExecutor;

    Dispatcher(String name, int maxNumberOfThreads) {
        workerExecutor = new Executor(regularQueue, "ports-worker-" + name, maxNumberOfThreads);
    }

    void dispatch(Consumer eventPort, Object payload, Object mutexSubject) {
        Task task = new Task(eventPort, payload, mutexSubject);

        int queueSize;

        synchronized (regularQueue) {
            regularQueue.offerLast(task);
            queueSize = regularQueue.size();
        }

        workerExecutor.onNewTaskAvailable(queueSize);
    }

    <I, O> PortsFuture<O> dispatch(Function<I, O> requestPort, I payload, Object mutexSubject) {
        Task task = new Task(requestPort, payload, mutexSubject);

        int queueSize;

        synchronized (regularQueue) {
            regularQueue.offerLast(task);
            queueSize = regularQueue.size();
        }

        workerExecutor.onNewTaskAvailable(queueSize);

        return new PortsFuture<>(task);
    }

    Task poll() {
        synchronized (regularQueue) {
            return regularQueue.pollFirst();
        }
    }

    void awaitQuiescence() {
        workerExecutor.awaitQuiescence();
    }
}
