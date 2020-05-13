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

@SuppressWarnings({"rawtypes", "unchecked"})
class MessageQueue {

    private class DispatchThread extends Thread {

        public DispatchThread(String name) {
            setDaemon(true);
            setName("ports-dispatcher-" + name);
            start();
        }

        @Override
        public void run() {
            while (true) {
                Task task;

                synchronized (queue) {
                    while (queue.isEmpty()) {
                        try {
                            queue.wait();
                        } catch (InterruptedException e) {
                            Ports.printWarning("dispatcher has been interrupted");
                            return;
                        }
                    }
                }

                workerExecutor.onNewTaskAvailable(1);
            }
        }
    }

    private final Deque<Task> queue = new ArrayDeque<>();
//    private final DispatchThread dispatchThread;
    private final Executor workerExecutor;

    MessageQueue(String name, int maxNumberOfThreads) {
//        dispatchThread = new DispatchThread(name);
        workerExecutor = new Executor(this, "ports-worker-" + name, maxNumberOfThreads);
    }

    void enqueue(Consumer eventPort, Object payload) {
        Task task = new Task(eventPort, payload);

        synchronized (queue) {
            queue.add(task);
//            queue.notify();
            workerExecutor.onNewTaskAvailable(queue.size());
        }
    }

    <I, O> PortsFuture<O> enqueue(Function<I, O> requestPort, I payload) {
        Task task = new Task(requestPort, payload);

        synchronized (queue) {
            queue.add(task);
//            queue.notify();
            workerExecutor.onNewTaskAvailable(queue.size());
        }

        return new PortsFuture<>(task);
    }

    Task poll() {
        synchronized (queue) {
            return queue.poll();
        }
    }

    void awaitQuiescence() {
        workerExecutor.awaitQuiescence();
    }
}
