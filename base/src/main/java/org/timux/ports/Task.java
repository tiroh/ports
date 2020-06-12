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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings({"unchecked", "rawtypes"})
class Task implements Runnable {

    private static final long[] TIMEOUTS_MS = {
            1, 1, 1, 2, 2, 2, 5, 5, 5,
            10, 10, 10, 20, 20, 20, 50, 50, 50,
            100, 100, 100, 200, 200, 200, 500
    };

    private final Consumer eventPort;
    private final Function requestPort;
    private final Object payload;
    private Object response;
    private boolean hasReturned = false;
    private Throwable throwable;

    private final Object sender;
    private final Object receiver;

    private final Thread createdByThread;
    private final Lock lock;

    private Thread processedByThread;

    Task(Consumer eventPort, Object payload, Object mutexSubject, Object sender, Object receiver) {
        this.eventPort = eventPort;
        this.requestPort = null;
        this.payload = payload;

        this.sender = sender;
        this.receiver = receiver;

        createdByThread = Thread.currentThread();

        lock = mutexSubject != null
                ? LockManager.getLock(mutexSubject)
                : null;
    }

    Task(Function requestPort, Object payload, Object mutexSubject, Object sender, Object receiver) {
        this.eventPort = null;
        this.requestPort = requestPort;
        this.payload = payload;

        this.sender = sender;
        this.receiver = receiver;

        createdByThread = Thread.currentThread();

        lock = mutexSubject != null
                ? LockManager.getLock(mutexSubject)
                : null;
    }

    Task(Throwable throwable) {
        this.throwable = throwable;

        hasReturned = true;

        eventPort = null;
        requestPort = null;
        payload = null;
        sender = null;
        receiver = null;

        createdByThread = Thread.currentThread();

        lock = null;
    }

    Thread getCreatedByThread() {
        return createdByThread;
    }

    public void setProcessedByThread(Thread processedByThread) {
        this.processedByThread = processedByThread;
    }

    Lock getLock() {
        return lock;
    }

    @Override
    public void run() {
        /*
         * Events are able to block requests. This happens when they are dispatched synchronously as
         * a simple method call. Therefore, a check for deadlocks must ALWAYS be performed, regardless
         * of whether the task handles a request or an event.
         */

        if (!hasReturned) {
            Executor.WorkerThread processedByWorkerThread = (processedByThread instanceof Executor.WorkerThread)
                    ? (Executor.WorkerThread) processedByThread
                    : null;

            if (lock == null) {
                try {
                    if (eventPort != null) {
                        eventPort.accept(payload);
                    } else {
                        response = requestPort.apply(payload);
                    }
                } catch (Exception e) {
                    throwable = e;
                }
            } else {
                if (lock.tryLock()) {
                    if (processedByWorkerThread != null) {
                        processedByWorkerThread.addCurrentLock(lock);
                    } else {
                        LockManager.addLockForPlainThread(processedByThread, lock);
                    }

                    try {
                        if (eventPort != null) {
                            eventPort.accept(payload);
                        } else {
                            response = requestPort.apply(payload);
                        }
                    } catch (Exception e) {
                        throwable = e;
                    } finally {
                        if (processedByWorkerThread != null) {
                            processedByWorkerThread.removeCurrentLock(lock);
                        } else {
                            LockManager.removeLockForPlainThread(processedByThread, lock);
                        }

                        lock.unlock();
                    }
                } else {
                    Task deadlockStart = LockManager.isDeadlocked(this, null, lock);

                    if (deadlockStart != null) {
                        printDeadlockWarning(deadlockStart);

                        try {
                            if (eventPort != null) {
                                eventPort.accept(payload);
                            } else {
                                response = requestPort.apply(payload);
                            }
                        } catch (Exception e) {
                            throwable = e;
                        }
                    } else {
                        int timeoutIdx = 0;

                        for (;;) {
                            boolean isAcquired = false;

                            try {
                                isAcquired = lock.tryLock(TIMEOUTS_MS[timeoutIdx], TimeUnit.MILLISECONDS);
                            } catch (InterruptedException e) {
                                //
                            }

                            timeoutIdx++;

                            if (!isAcquired) {
                                deadlockStart = LockManager.isDeadlocked(this, null, lock);

                                if (deadlockStart != null) {
                                    printDeadlockWarning(deadlockStart);

                                    try {
                                        if (eventPort != null) {
                                            eventPort.accept(payload);
                                        } else {
                                            response = requestPort.apply(payload);
                                        }
                                    } catch (Exception e) {
                                        throwable = e;
                                    }

                                    break;
                                }
                            } else {
                                if (processedByWorkerThread != null) {
                                    processedByWorkerThread.addCurrentLock(lock);
                                } else {
                                    LockManager.addLockForPlainThread(processedByThread, lock);
                                }

                                try {
                                    if (eventPort != null) {
                                        eventPort.accept(payload);
                                    } else {
                                        response = requestPort.apply(payload);
                                    }
                                } catch (Exception e) {
                                    throwable = e;
                                } finally {
                                    if (processedByWorkerThread != null) {
                                        processedByWorkerThread.removeCurrentLock(lock);
                                    } else {
                                        LockManager.removeLockForPlainThread(processedByThread, lock);
                                    }

                                    lock.unlock();
                                }

                                break;
                            }

                            if (timeoutIdx >= TIMEOUTS_MS.length) {
                                lock.lock();

                                if (processedByWorkerThread != null) {
                                    processedByWorkerThread.addCurrentLock(lock);
                                } else {
                                    LockManager.addLockForPlainThread(processedByThread, lock);
                                }

                                try {
                                    if (eventPort != null) {
                                        eventPort.accept(payload);
                                    } else {
                                        response = requestPort.apply(payload);
                                    }
                                } catch (Exception e) {
                                    throwable = e;
                                } finally {
                                    if (processedByWorkerThread != null) {
                                        processedByWorkerThread.removeCurrentLock(lock);
                                    } else {
                                        LockManager.removeLockForPlainThread(processedByThread, lock);
                                    }

                                    lock.unlock();
                                }

                                break;
                            }
                        }
                    }
                }
            }
        }

        processedByThread = null;

        synchronized (this) {
            hasReturned = true;
            notify();
        }
    }

    boolean hasReturned() {
        return hasReturned;
    }

    Throwable getThrowable() {
        return throwable;
    }

    Object getResponse() {
        return response;
    }

    synchronized Object waitForResponse() {
        while (!hasReturned) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new ExecutionException(e);
            }
        }

        if (throwable != null) {
            throw new ExecutionException(throwable);
        }

        return response;
    }

    synchronized Object waitForResponse(long timeout, TimeUnit unit) throws TimeoutException {
        long waitMillis = unit.toMillis(timeout);

        while (!hasReturned) {
            long startMillis = System.currentTimeMillis();

            try {
                wait(waitMillis);
            } catch (InterruptedException e) {
                throw new ExecutionException(e);
            }

            if (!hasReturned) {
                long passedMillis = System.currentTimeMillis() - startMillis;

                if (passedMillis >= timeout - 1) {
                    throw new TimeoutException();
                }

                waitMillis -= passedMillis;
            }
        }

        if (throwable != null) {
            throw new ExecutionException(throwable);
        }

        return response;
    }

    void printDeadlockWarning(Task deadlockStart) {
        if (Executor.TEST_API_DISABLE_DEADLOCK_WARNINGS) {
            return;
        }

        List<Task> chain = new ArrayList<>();

        Task t = this;

        for (;;) {
            chain.add(t);

            if (t == deadlockStart) {
                break;
            }

            t = ((Executor.WorkerThread) t.createdByThread).getCurrentTask();
        }

        for (int i = 0; i < chain.size() / 2; i++) {
            t = chain.get(i);
            int idx = chain.size() - i - 1;
            chain.set(i, chain.get(idx));
            chain.set(idx, t);
        }

        String message = chain.stream()
                .map(task -> task.sender.getClass().getName() + "(" + task.payload.getClass().getName() + ")")
                .collect(Collectors.joining(" -> "));


        Ports.printWarning(String.format("deadlock detected: %s -> %s",
                message,
                receiver.getClass().getName()));
        Ports.printWarning("    Deadlock resolution may cause a loss of data integrity. It is strongly recommended to remove the cause.");
    }
}
