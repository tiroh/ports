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

    class WorkerThread extends Thread {

        private Runnable task;

        public WorkerThread(ThreadGroup threadGroup) {
            super(threadGroup, threadGroup.getName() + "-" + nextThreadId.getAndIncrement());
            setDaemon(true);
            start();
        }

        public void setTask(Runnable task) {
            this.task = task;
        }

        @Override
        public void run() {
            while (true) {
                synchronized (this) {
                    while (task == null) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            synchronized (availableThreads) {
                                availableThreads.remove(this);
                                numberOfThreads--;
                            }

                            return;
                        }
                    }
                }

                task.run();
                task = null;

                synchronized (availableThreads) {
                    availableThreads.push(this);
                }
            }
        }
    }

    private final Deque<WorkerThread> availableThreads = new ArrayDeque<>();
    private final ThreadGroup threadGroup;
    private final AtomicInteger nextThreadId = new AtomicInteger();
    private int numberOfThreads = 0;

    public Executor(String threadGroupName) {
        threadGroup = new ThreadGroup(threadGroupName);
    }

    public void submit(Task task) {
        WorkerThread workerThread;

        synchronized (availableThreads) {
            if (availableThreads.isEmpty()) {
                WorkerThread newThread = new WorkerThread(threadGroup);
                numberOfThreads++;
                availableThreads.push(newThread);
            }

            workerThread = availableThreads.pollFirst();
        }

        workerThread.setTask(task);

        synchronized (workerThread) {
            workerThread.notify();
        }
    }

    public boolean isOwnThread(Thread thread) {
        return threadGroup == thread.getThreadGroup();
    }

    public void awaitQuiescence() {
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

    public boolean isQuiescent() {
        synchronized (availableThreads) {
            return availableThreads.size() == numberOfThreads;
        }
    }
}
