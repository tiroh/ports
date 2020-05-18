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

//    private class DeadlockDetector extends Thread {
//
//        public DeadlockDetector(String name) {
//            setDaemon(true);
//            setName(name);
//            start();
//        }
//
//        @Override
//        public void run() {
//            while (true) {
//                Task task;
//
//                synchronized (deadlockCheckQueue) {
//                    while (deadlockCheckQueue.isEmpty()) {
//                        try {
//                            System.out.println(getName() + " waits");
//                            deadlockCheckQueue.wait();
//                            System.out.println(getName() + " awakes");
//                        } catch (InterruptedException e) {
//                            Ports.printWarning("dispatcher has been interrupted");
//                            return;
//                        }
//                    }
//
//                    task = deadlockCheckQueue.pollFirst();
//                }
//
//                RequestToken token = TokenManager.requestTokens.computeIfAbsent(task.receiverClass, key -> new RequestToken(0L));
//
//                if (task.requestToken == token.value.get()) {
//                    System.out.println(getName() + ", deadlock detected " + task.requestToken);
//                    task.isDeadlocked = true;
//
//                    int queueSize;
//
//                    synchronized (deadlockedQueue) {
//                        deadlockedQueue.offerLast(task);
//                        queueSize = deadlockedQueue.size();
//                    }
//
//                    deadlockResolutor.onNewTaskAvailable(queueSize);
//                } else {
//                    System.out.println(getName() + ", no deadlock " + task.requestToken);
//                    token.value.set(task.requestToken);
//
//                    int queueSize;
//
//                    synchronized (regularQueue) {
//                        regularQueue.offerLast(task);
//                        queueSize = regularQueue.size();
//                    }
//
//                    workerExecutor.onNewTaskAvailable(queueSize);
//                }
//            }
//        }
//    }

//    private final Deque<Task> deadlockCheckQueue = new ArrayDeque<>();
//    private final Deque<Task> deadlockedQueue = new ArrayDeque<>();
    private final Deque<Task> regularQueue = new ArrayDeque<>();
//    private final DeadlockDetector deadlockDetector;
    private final Executor workerExecutor;
//    private final Executor deadlockResolutor;

    Dispatcher(String name, int maxNumberOfThreads) {
//        deadlockDetector = new DeadlockDetector("ports-dispatcher-" + name);
        workerExecutor = new Executor(regularQueue, "ports-worker-" + name, maxNumberOfThreads);
//        deadlockResolutor = new Executor(deadlockedQueue, "ports-deadlock-" + name, Integer.MAX_VALUE);
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

//        synchronized (deadlockCheckQueue) {
//            deadlockCheckQueue.offerLast(task);
//            deadlockCheckQueue.notify();
//        }

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
