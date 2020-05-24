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

class Dispatcher {

    private final Deque<Task> queue = new ArrayDeque<>();

    private final Executor workerExecutor;

    Dispatcher(String name, int maxNumberOfThreads) {
        workerExecutor = maxNumberOfThreads > 0
                ? new Executor(this, "ports-worker-" + name, maxNumberOfThreads)
                : null;
    }

    <T> void dispatch(Consumer<T> eventPort, T payload, Object mutexSubject) {
        Task task = new Task(eventPort, payload, mutexSubject);

        if (workerExecutor == null || task.getCreatedByThread().getThreadGroup() == workerExecutor.getThreadGroup()) {
            task.processedByThread = task.getCreatedByThread();
            task.run();
            return;
        }

        synchronized (queue) {
            queue.offerLast(task);
            workerExecutor.onNewEventTaskAvailable(task, queue.size());
        }
    }

    <I, O> PortsFuture<O> dispatch(Function<I, O> requestPort, I payload, Object mutexSubject) {
        Task task = new Task(requestPort, payload, mutexSubject);

        if (workerExecutor == null || task.getCreatedByThread().getThreadGroup() == workerExecutor.getThreadGroup()) {
            task.processedByThread = task.getCreatedByThread();
            task.run();
            return new PortsFuture<>(task);
        }

        synchronized (queue) {
            queue.offerLast(task);
            workerExecutor.onNewRequestTaskAvailable(task, queue.size());
        }

        return new PortsFuture<>(task);
    }

    Task poll() {
        synchronized (queue) {
            Task task = queue.pollFirst();
            return task;
        }
    }

    int getNumberOfThreadsCreated() {
        return workerExecutor != null ? workerExecutor.getNumberOfThreadsCreated() : 0;
    }

    void awaitQuiescence() {
        if (workerExecutor != null) {
            workerExecutor.awaitQuiescence();
        }
    }
}
