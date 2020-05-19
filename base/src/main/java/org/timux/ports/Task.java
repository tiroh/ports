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

    Task(Throwable throwable, Object mutexSubject) {
        this.eventPort = null;
        this.requestPort = null;
        this.payload = null;
        this.throwable = throwable;
        this.hasReturned = true; // FIXME race condition
        this.mutexSubject = mutexSubject;

        createdByThread = Thread.currentThread();
    }

    Thread getCreatedByThread() {
        return createdByThread;
    }

    @Override
    public void run() {
        if (!hasReturned) {
            try {
                if (mutexSubject == null) {
                    if (eventPort != null) {
                        eventPort.accept(payload);
                    } else {
                        response = requestPort.apply(payload);
                    }
                } else {
                    Executor.WorkerThread processedByWorkerThread = (processedByThread instanceof Executor.WorkerThread)
                            ? (Executor.WorkerThread) processedByThread
                            : null;

                    Lock lock = LockManager.getLock(mutexSubject);

                    if (eventPort != null) {
                        lock.lock();

                        try {
                            eventPort.accept(payload);
                        } catch (Throwable t) {
                            throwable = t;
                        } finally {
                            lock.unlock();
                        }
                    } else {
                        if (lock.tryLock()) {
                            if (processedByWorkerThread != null) {
                                processedByWorkerThread.addCurrentLock(lock);
                            } else {
                                LockManager.addLockForPlainThread(processedByThread, lock);
                            }

                            try {
                                response = requestPort.apply(payload);
                            } catch (Throwable t) {
                                throwable = t;
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

                            if (LockManager.isDeadlocked(processedByThread, lock)) {
                                if (processedByWorkerThread != null) {
                                    processedByWorkerThread.removeCurrentLock(lock);
                                } else {
                                    LockManager.removeLockForPlainThread(processedByThread, lock);
                                }

                                response = requestPort.apply(payload);
                            } else {
                                lock.lock();

                                try {
                                    response = requestPort.apply(payload);
                                } catch (Throwable t) {
                                    throwable = t;
                                } finally {
                                    if (processedByWorkerThread != null) {
                                        processedByWorkerThread.removeCurrentLock(lock);
                                    } else {
                                        LockManager.removeLockForPlainThread(processedByThread, lock);
                                    }

                                    lock.unlock();
                                }
                            }
                        }

                    }
                }
            } catch (Throwable t) {
                throwable = t;
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
