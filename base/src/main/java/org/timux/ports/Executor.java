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
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class Executor {

    private static final long IDLE_LIFETIME_MS = 2000;

    // Must not be private or final because it is modified by the tests to achieve
    // deterministic behavior.
    static int TEST_API_MAX_NUMBER_OF_THREADS = -1;

    class WorkerThread extends Thread implements Thread.UncaughtExceptionHandler {

        private Task task;

        public WorkerThread(ThreadGroup threadGroup) {
            super(threadGroup, threadGroup.getName() + "-" + nextThreadId.getAndIncrement());
            setDaemon(true);
            setUncaughtExceptionHandler(this);
            start();
        }

        @Override
        public void run() {
            while (true) {
                boolean permitAcquired;

                try {
                    permitAcquired = poolSemaphore.tryAcquire(IDLE_LIFETIME_MS, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    synchronized (threadPool) {
                        threadPool.remove(this);
                        return;
                    }
                }

                if (!permitAcquired) {
                    synchronized (threadPool) {
                        if (threadPool.size() == 1) {
                            // Do not kill this thread because it is the only one left.
                            continue;
                        }

                        threadPool.remove(this);
                        return;
                    }
                }

                synchronized (threadPool) {
                    numberOfBusyThreads++;
                }

                task = messageQueue.poll();

                // Exception handling is done within the task, so not required here.
                task.run();
                task = null;

                synchronized (threadPool) {
                    numberOfBusyThreads--;
                }
            }
        }

        @Override
        public void uncaughtException(Thread thread, Throwable t) {
            // This should never happen because we catch all exceptions.

            synchronized (threadPool) {
                threadPool.remove(thread);
            }

            System.err.println("Thread [" + thread.getName() + "] died because of uncaught exception:");
            t.printStackTrace();
        }
    }

    private final Deque<WorkerThread> threadPool = new ArrayDeque<>();
    private final ThreadGroup threadGroup;
    private final MessageQueue messageQueue;
    private final AtomicInteger nextThreadId = new AtomicInteger();
    private final int maxNumberOfThreads;
    private final Semaphore poolSemaphore = new Semaphore(0);

    private int numberOfBusyThreads = 0;

    Executor(MessageQueue messageQueue, String threadGroupName, int maxNumberOfThreads) {
        this.messageQueue = messageQueue;
        this.threadGroup = new ThreadGroup(threadGroupName);
        this.maxNumberOfThreads = TEST_API_MAX_NUMBER_OF_THREADS < 0 ? maxNumberOfThreads : TEST_API_MAX_NUMBER_OF_THREADS;
    }

    void onNewTaskAvailable(int numberOfTasksInQueue) {
        synchronized (threadPool) {
            if (numberOfTasksInQueue > threadPool.size() - numberOfBusyThreads && threadPool.size() < maxNumberOfThreads) {
                threadPool.push(new WorkerThread(threadGroup));
            }
        }

        poolSemaphore.release();
    }

    void awaitQuiescence() {
        for (int numberOfRuns = 0; ; numberOfRuns = (numberOfRuns + 1) & 0xffffff) {
            synchronized (threadPool) {
                if (numberOfBusyThreads == 0) {
                    return;
                }
            }

            try {
                Thread.sleep(numberOfRuns < 10 ? 10 : (numberOfRuns < 50 ? 100 : 333));
            } catch (InterruptedException e) {
                Ports.printWarning("awaitQuiescence has been interrupted");
                return;
            }
        }
    }

    boolean isQuiescent() {
        synchronized (threadPool) {
            return numberOfBusyThreads == 0;
        }
    }

    int getNumberOfThreads() {
        synchronized (threadPool) {
            return threadPool.size();
        }
    }
}
