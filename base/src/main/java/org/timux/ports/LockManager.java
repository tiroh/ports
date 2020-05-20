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
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class LockManager {

    // TODO optimize this for better concurrency
    private static final Map<Object, Lock> locks = new WeakHashMap<>(128);

    private static final Map<Thread, List<Lock>> plainThreadLocks = new WeakHashMap<>();

    static Lock getLock(Object subject) {
        synchronized (locks) {
            return locks.computeIfAbsent(subject, key -> new ReentrantLock(false));
        }
    }

    static void addLockForPlainThread(Thread thread, Lock lock) {
        List<Lock> lockList;

        synchronized (plainThreadLocks) {
            lockList = plainThreadLocks.computeIfAbsent(thread, key -> new ArrayList<>());
            lockList.add(lock);
        }
    }

    static void removeLockForPlainThread(Thread thread, Lock lock) {
        synchronized (plainThreadLocks) {
            List<Lock> lockList = plainThreadLocks.get(thread);

            if (lockList != null) {
                lockList.remove(lock);
            }
        }
    }

    static boolean isDeadlocked(Thread thread, Lock wantedLock) {
        while (thread instanceof Executor.WorkerThread) {
            Executor.WorkerThread workerThread = (Executor.WorkerThread) thread;

            if (workerThread.containsLock(wantedLock)) {
                return true;
            }

            thread = workerThread.getCurrentTask().getCreatedByThread();
        }

        synchronized (plainThreadLocks) {
            List<Lock> lockList = plainThreadLocks.get(thread);
            return lockList != null && lockList.contains(wantedLock);
        }

        // T1(A) -> T2(B) -> T3(A?)
    }
}
