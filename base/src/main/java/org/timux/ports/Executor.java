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
import java.util.concurrent.atomic.AtomicInteger;

class Executor {

    private static final long IDLE_LIFETIME_MS = 1300;

    class WorkerThread extends Thread implements Thread.UncaughtExceptionHandler {

        private Task task;

        public WorkerThread(ThreadGroup threadGroup) {
            super(threadGroup, threadGroup.getName() + "-" + nextThreadId.getAndIncrement());
            setDaemon(true);
            setUncaughtExceptionHandler(this);
            start();
        }

        public void setTask(Task task) {
            this.task = task;
        }

        @Override
        public void run() {
            while (true) {
                synchronized (this) {
                    long remainingWaitTime = IDLE_LIFETIME_MS;

                    while (task == null) {
                        long startTime = System.currentTimeMillis();

                        try {
                            wait(remainingWaitTime);
                        } catch (InterruptedException e) {
                            synchronized (availableThreads) {
                                availableThreads.remove(this);
                                numberOfThreads--;
                                return;
                            }
                        }

                        if (task != null) {
                            break;
                        }

                        remainingWaitTime -= System.currentTimeMillis() - startTime;

                        if (remainingWaitTime <= 0) {
                            synchronized (availableThreads) {
                                // It could be that right before we entered this sync block,
                                // this thread got popped from the stack,
                                // so we have to check this race condition.

                                if (task == null) {
                                    availableThreads.remove(this);
                                    numberOfThreads--;
                                    return;
                                }
                            }
                        }
                    }
                }

                // Exception handling is done within the task, so not required here.
                task.run();
                task = null;

                synchronized (availableThreads) {
                    availableThreads.push(this);
                }
            }
        }

        @Override
        public void uncaughtException(Thread thread, Throwable t) {
            // This should never happen because we catch all exceptions.

            synchronized (availableThreads) {
                numberOfThreads--;
            }

            System.err.println("Thread [" + thread.getName() + "] died because of uncaught exception:");
            t.printStackTrace();
        }
    }

    private final Deque<WorkerThread> availableThreads = new ArrayDeque<>();
    private final ThreadGroup threadGroup;
    private final AtomicInteger nextThreadId = new AtomicInteger();
    private int numberOfThreads = 0;

    Executor(String threadGroupName) {
        threadGroup = new ThreadGroup(threadGroupName);
    }

    void submit(Task task) {
        WorkerThread workerThread;

        synchronized (availableThreads) {
            if (availableThreads.isEmpty()) {
                WorkerThread newThread = new WorkerThread(threadGroup);
                numberOfThreads++;
                availableThreads.push(newThread);
            }

            workerThread = availableThreads.pollFirst();
            workerThread.setTask(task);
        }

        synchronized (workerThread) {
            workerThread.notify();
        }
    }

    boolean isOwnThread(Thread thread) {
        return threadGroup == thread.getThreadGroup();
    }

    void awaitQuiescence() {
        for (int numberOfRuns = 0; ; numberOfRuns = (numberOfRuns + 1) & 0xffffff) {
            synchronized (availableThreads) {
                if (availableThreads.size() == numberOfThreads) {
                    return;
                }
            }

            try {
                Thread.sleep(numberOfRuns < 10 ? 10 : (numberOfRuns < 50 ? 100 : 333));
            } catch (InterruptedException e) {
                //
            }
        }
    }

    boolean isQuiescent() {
        synchronized (availableThreads) {
            return availableThreads.size() == numberOfThreads;
        }
    }

    int getNumberOfThreads() {
        return numberOfThreads;
    }
}
