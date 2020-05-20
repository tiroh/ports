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

    private final Deque<Task> queue = new ArrayDeque<>();
    private final Executor workerExecutor;

    Dispatcher(String name, int maxNumberOfThreads) {
        workerExecutor = maxNumberOfThreads > 0
                ? new Executor(this, "ports-worker-" + name, maxNumberOfThreads)
                : null;
    }

    void dispatch(Consumer eventPort, Object payload, Object mutexSubject) {
        Task task = new Task(eventPort, payload, mutexSubject);

        if (workerExecutor == null) {
            task.processedByThread = Thread.currentThread();
            task.run();
            return;
        }

        synchronized (queue) {
            queue.offerLast(task);
            workerExecutor.onNewTaskAvailable(task, queue.size());
        }
    }

    <I, O> PortsFuture<O> dispatch(Function<I, O> requestPort, I payload, Object mutexSubject) {
        Task task = new Task(requestPort, payload, mutexSubject);

        if (workerExecutor == null) {
            task.processedByThread = Thread.currentThread();
            task.run();
            return new PortsFuture<>(task);
        }

        synchronized (queue) {
            queue.offerLast(task);
            workerExecutor.onNewTaskAvailable(task, queue.size());
        }

        return new PortsFuture<>(task);
    }

    Task poll() {
        synchronized (queue) {
            return queue.pollFirst();
        }
    }

    void awaitQuiescence() {
        if (workerExecutor != null) {
            workerExecutor.awaitQuiescence();
        }
    }
}
