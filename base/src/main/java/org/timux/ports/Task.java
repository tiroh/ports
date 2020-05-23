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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings({"unchecked", "rawtypes"})
class Task implements Runnable {

    private static final long[] TIMEOUTS_MS = {1, 2, 5, 10, 100, 200, 500, 1000, 1000, 1000, 2000, 2000, 2000, 3000};

    private final Consumer eventPort;
    private final Function requestPort;
    private final Object payload;
    private Object response;
    private boolean hasReturned = false;
    private Throwable throwable;

    private final Thread createdByThread;

    Thread processedByThread;

    final Object mutexSubject;

    Task(Consumer eventPort, Object payload, Object mutexSubject) {
        this.eventPort = eventPort;
        this.requestPort = null;
        this.payload = payload;
        this.mutexSubject = mutexSubject;

        createdByThread = Thread.currentThread();
    }

    Task(Function requestPort, Object payload, Object mutexSubject) {
        this.eventPort = null;
        this.requestPort = requestPort;
        this.payload = payload;
        this.mutexSubject = mutexSubject;

        createdByThread = Thread.currentThread();
    }

    Thread getCreatedByThread() {
        return createdByThread;
    }

    public Thread getProcessedByThread() {
        return processedByThread;
    }

    @Override
    public void run() {
        if (!hasReturned) {
//            System.out.println("    " + processedByThread + " starts -- "
//                    + (eventPort != null ? "event " + payload : "request " + payload));

            Executor.WorkerThread processedByWorkerThread = (processedByThread instanceof Executor.WorkerThread)
                    ? (Executor.WorkerThread) processedByThread
                    : null;

            if (processedByWorkerThread != null) {
                processedByWorkerThread.addCurrentTask(this);
            }

            if (mutexSubject == null) {
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
                Lock lock = LockManager.getLock(mutexSubject);

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
                    // TODO optimize this (see Executor)

                    if (LockManager.isDeadlocked(processedByThread, null, lock)) {
//                        System.out.println("    resolver C -- " + processedByThread);

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
//                        System.out.println("waiting " + payload + " " + processedByThread);

                        int timeoutIdx = 0;

                        for(;;) {
                            boolean isAcquired = false;

                            try {
//                                System.out.println("    waiting " + TIMEOUTS_MS[timeoutIdx]);
                                isAcquired = lock.tryLock(TIMEOUTS_MS[timeoutIdx], TimeUnit.MILLISECONDS);
                            } catch (InterruptedException e) {
                                //
                            }

                            if (timeoutIdx != TIMEOUTS_MS.length - 1) {
                                timeoutIdx++;
                            }

                            if (!isAcquired) {
                                if (LockManager.isDeadlocked(processedByThread, null, lock)) {
//                                    System.out.println("resolver D");

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

                                    break;
                                }
                            }
                        }
                    }
                }
            }

//            System.out.println("    " + processedByThread + " exits");

            if (processedByWorkerThread != null) {
                processedByWorkerThread.removeCurrentTask(this);
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
}
