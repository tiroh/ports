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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class LockManager {

    private static final ConcurrentMap<WeakKey, Lock> locks =
            new ConcurrentHashMap<>(128, 0.75f, Runtime.getRuntime().availableProcessors());

    static Lock getLock(Object subject) {
        return locks.computeIfAbsent(new WeakKey(subject), key -> new ReentrantLock(false));
    }

    static boolean isDeadlocked(Thread thread, Lock wantedLock) {
        while (thread instanceof Executor.WorkerThread) {
            Executor.WorkerThread workerThread = (Executor.WorkerThread) thread;

            if (workerThread.getCurrentLock() == wantedLock) {
                return true;
            }

            thread = workerThread.getCurrentTask().getCreatedByThread();
        }

        return false;

        // T1(A) -> T2(B) -> T3(A?)
    }
}
