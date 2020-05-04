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

    private static class DispatchThread extends Thread {

        public DispatchThread() {
            setDaemon(true);
            setName("ports-dispatcher");
            start();
        }

        @Override
        public void run() {
            while (true) {
                Task task;

                synchronized (messageQueue) {
                    while (messageQueue.isEmpty()) {
                        try {
                            messageQueue.wait();
                        } catch (InterruptedException e) {
                            //
                        }
                    }

                    task = messageQueue.poll();
                }

                workerExecutor.submit(task);
            }
        }
    }

    private static final DispatchThread dispatchThread = new DispatchThread();
    private static final Executor workerExecutor = new Executor("ports-worker");
    private static final Deque<Task> messageQueue = new ArrayDeque<>();

    static void enqueue(Consumer eventPort, Object payload) {
        if (workerExecutor.isOwnThread(Thread.currentThread())) {
            System.out.println("---- easy: " + payload);
            eventPort.accept(payload);
            return;
        } else {
            System.out.println("---- new entry: " + payload);
        }

        Task task = new Task(eventPort, null, payload);

        synchronized (messageQueue) {
            messageQueue.add(task);
            messageQueue.notify();
        }

        waitForResponse(task);
    }

    static void enqueueAsync(Consumer eventPort, Object payload) {
        Task task = new Task(eventPort, null, payload);
        workerExecutor.submit(task);
    }

    static <I, O> O enqueue(Function<I, O> requestPort, I payload) {
        if (workerExecutor.isOwnThread(Thread.currentThread())) {
            System.out.println("---- easy: " + payload);
            return requestPort.apply(payload);
        } else {
            System.out.println("---- new entry: " + payload);
        }

        Task task = new Task(null, requestPort, payload);

        synchronized (messageQueue) {
            messageQueue.add(task);
            messageQueue.notify();
        }

        return (O) waitForResponse(task);
    }

    private static Object waitForResponse(Task task) {
        synchronized (task) {
            while (!task.hasReturned()) {
                try {
                    task.wait();
                } catch (InterruptedException e) {
                    //
                }
            }

            return task.getResponse();
        }
    }
}
