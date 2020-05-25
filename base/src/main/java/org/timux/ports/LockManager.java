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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class LockManager {

    private static final ConcurrentWeakHashMap<Object, Lock> subjectLocks = new ConcurrentWeakHashMap<>();
    private static final ConcurrentWeakHashMap<Thread, List<Lock>> plainThreadLocks = new ConcurrentWeakHashMap<>();

    private static final Map<Thread, Object> seenThreads = new HashMap<>(128);
    private static final Object mapValue = new Object();

    static Lock getLock(Object subject) {
        return subjectLocks.computeIfAbsent(subject, key -> new ReentrantLock(false));
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
        if (Thread.currentThread() instanceof Executor.WorkerThread) {
            Executor.WorkerThread w = ((Executor.WorkerThread) Thread.currentThread());
            Map<Thread, Object> m = w.getSeenThreads();
            boolean result = isDeadlocked0(taskThread, m, targetGroup, wantedLock);
            m.clear();
            return result;
        }

        synchronized (seenThreads) {
            seenThreads.clear();
            return isDeadlocked0(taskThread, seenThreads, targetGroup, wantedLock);
        }
    }

    private static boolean isDeadlocked0(Thread taskThread, Map<Thread, Object> seenThreads, ThreadGroup targetGroup, Lock wantedLock) {
        if (seenThreads.containsKey(taskThread)) {
            return false;
        }

        seenThreads.put(taskThread, mapValue);

        if (taskThread instanceof Executor.WorkerThread) {
            Executor.WorkerThread workerThread = (Executor.WorkerThread) taskThread;

            if (workerThread.hasLock(wantedLock)) {
                return true;
            }

            Task task = workerThread.getCurrentTask();

            if (task == null) {
                return false;
            }

            Thread createdByThread = task.getCreatedByThread();

            if (targetGroup != null && targetGroup == createdByThread.getThreadGroup()) {
                return true;
            }

            return isDeadlocked0(createdByThread, seenThreads, targetGroup, wantedLock);
        } else {
            List<Lock> lockList = plainThreadLocks.get(taskThread);

            if (lockList != null) {
                synchronized (lockList) {
                    return lockList.contains(wantedLock);
                }
            }
        }

        return false;

        // T1(A) -> T2(B) -> T3(A?)
    }
}
