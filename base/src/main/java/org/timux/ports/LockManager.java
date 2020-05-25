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
    private static final Object DUMMY_VALUE = new Object();

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

    static Task isDeadlocked(Task task, ThreadGroup targetGroup, Lock wantedLock) {
        if (Thread.currentThread() instanceof Executor.WorkerThread) {
            Executor.WorkerThread w = ((Executor.WorkerThread) Thread.currentThread());
            Map<Thread, Object> m = w.getSeenThreads();
            Task result = isDeadlocked0(task, m, targetGroup, wantedLock);
            m.clear();
            return result;
        }

        synchronized (seenThreads) {
            Task result = isDeadlocked0(task, seenThreads, targetGroup, wantedLock);
            seenThreads.clear();
            return result;
        }
    }

    private static Task isDeadlocked0(Task task, Map<Thread, Object> seenThreads, ThreadGroup targetGroup, Lock wantedLock) {
        /*
         * The map 'seenThreads' is used as a set here. It is necessary because otherwise, we can enter infinite
         * loops.
         *
         * The reason is a race condition in which a thread T1 in domain D1 sends a message M1 to domain D2, while at
         * the same time a thread T2 in D2 sends a message M2 to D1. That means, M1 is created by T1, and M2 is created
         * by T2.
         *
         * Now, if coincidentally the roles of the two threads get reversed, i.e. T1 dispatches M2 and T2 dispatches
         * M1, we have a situation where the recursion below never terminates because of this mutual creation
         * relationship: the creator of M1 is T1, the message of T1 is M2, the creator of M2 is T2, the message of
         * T2 is M1, the creator of M1 is T1, etc. That is, the recursion bounces between the two messages.
         *
         * By storing all threads that we have seen so far, we can detect this situation and leave the recursion.
         */

        Thread createdByThread = task.getCreatedByThread();

        if (seenThreads.containsKey(createdByThread)) {
            return null;
        }

        if (targetGroup != null && targetGroup == createdByThread.getThreadGroup()) {
            return task;
        }

        seenThreads.put(createdByThread, DUMMY_VALUE);

        if (createdByThread instanceof Executor.WorkerThread) {
            Executor.WorkerThread workerThread = (Executor.WorkerThread) createdByThread;

            if (workerThread.hasLock(wantedLock)) {
                return task;
            }

            Task processedTask = workerThread.getCurrentTask();

            if (processedTask == null) {
                return null;
            }

            return isDeadlocked0(processedTask, seenThreads, targetGroup, wantedLock);
        } else {
            List<Lock> lockList = plainThreadLocks.get(createdByThread);

            if (lockList != null) {
                synchronized (lockList) {
                    return lockList.contains(wantedLock) ? task : null;
                }
            }
        }

        return null;

        // T1(A) -> T2(B) -> T3(A?)
    }
}
