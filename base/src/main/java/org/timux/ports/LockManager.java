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

import org.timux.ports.types.Container;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class LockManager {

    private static final ConcurrentWeakHashMap<Object, Lock> subjectLocks = new ConcurrentWeakHashMap<>();
    private static final ConcurrentWeakHashMap<Thread, List<Lock>> plainThreadLocks = new ConcurrentWeakHashMap<>();

    static Lock getLock(Object subject) {
        return subjectLocks.computeIfAbsent(subject, key -> new ReentrantLock(false));
    }

    static boolean tryLock(Thread thread, Object subject) {
        Container<Boolean> r = Container.of(Boolean.TRUE);

        subjectLocks.compute(
                subject,
                (key, currentLock) -> {
                    if (currentLock == null) {
                        Lock newLock = new ReentrantLock(false);
                        newLock.lock();
                        return newLock;
                    } else {
                        r.value = currentLock.tryLock();

                        if (r.value) {
                            if (thread instanceof Executor.WorkerThread) {
                                ((Executor.WorkerThread) thread).addCurrentLock(currentLock);
                            } else {
                                addLockForPlainThread(thread, currentLock);
                            }
                        }

                        return currentLock;
                    }
                });

        return r.value;
    }

    static void addLockForPlainThread(Thread thread, Lock lock) {
        List<Lock> lockList = plainThreadLocks.computeIfAbsent(thread, key -> new ArrayList<>());

        synchronized (lockList) {
            lockList.add(lock);
        }
    }

    static void removeLockForPlainThread(Thread thread, Lock lock) {
        List<Lock> lockList = plainThreadLocks.get(thread);

        if (lockList != null) {
            synchronized (lockList) {
                lockList.remove(lock);
            }
        }
    }

    static boolean isDeadlocked(Thread taskThread, ThreadGroup targetGroup, Lock wantedLock) {
        return isDeadlockedInternal(taskThread, targetGroup, wantedLock);

        // T1(A) -> T2(B) -> T3(A?)
    }

    static boolean isDeadlockedInternal(Thread thread, ThreadGroup targetGroup, Lock wantedLock) {
        if (thread instanceof Executor.WorkerThread) {
            Executor.WorkerThread workerThread = (Executor.WorkerThread) thread;

            if (workerThread.hasLock(wantedLock)) {
                return true;
            }

            List<Task> workerTasks = workerThread.getCurrentTasks();

            synchronized (workerTasks) {
                // In the meantime, the workerThreads could have finished working.
                if (workerTasks.isEmpty()) {
                    return false;
                }

                Thread lastThread = null;

                for (Task task : workerTasks) {
                    Thread createdByThread = task.getCreatedByThread();

                    if (createdByThread == thread) {
                        return false;
                    }

                    if (createdByThread == lastThread) {
                        return false;
                    }

                    lastThread = createdByThread;

                    if (targetGroup == createdByThread.getThreadGroup()) {
                        return true;
                    }

                    if (isDeadlockedInternal(createdByThread, targetGroup, wantedLock)) {
                        return true;
                    }
                }
            }
        }

        List<Lock> lockList = plainThreadLocks.get(thread);

        if (lockList != null) {
            synchronized (lockList) {
                return lockList.contains(wantedLock);
            }
        }

        return false;
    }
}
