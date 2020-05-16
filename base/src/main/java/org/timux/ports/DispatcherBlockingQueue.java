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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings({"rawtypes", "unchecked"})
class DispatcherBlockingQueue {

    private final BlockingQueue<Task> queue;
    private final ExecutorBlockingQueue workerExecutor;

    DispatcherBlockingQueue(String name, int maxNumberOfThreads) {
        queue = new ArrayBlockingQueue<>(512 + 512 * maxNumberOfThreads, false);
        workerExecutor = new ExecutorBlockingQueue(this, "ports-dispatcher-" + name, maxNumberOfThreads);
    }

    void dispatch(Consumer eventPort, Object payload) {
        Task task = new Task(eventPort, payload);

        synchronized (queue) {
            queue.add(task);
            workerExecutor.onNewTaskAvailable(queue.size());
        }
    }

    <I, O> PortsFuture<O> dispatch(Function<I, O> requestPort, I payload) {
        Task task = new Task(requestPort, payload);

        try {
            queue.put(task);
        } catch (InterruptedException e) {
            return new PortsFuture<>(e);
        }

        workerExecutor.onNewTaskAvailable(queue.size());

        return new PortsFuture<>(task);
    }

    Task poll(long timeout, TimeUnit timeUnit) {
        try {
            return queue.poll(timeout, timeUnit);
        } catch (InterruptedException e) {
            return new Task(e);
        }

    }

    void awaitQuiescence() {
        workerExecutor.awaitQuiescence();
    }
}
