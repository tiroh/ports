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

import org.timux.ports.types.Either;
import org.timux.ports.types.Either3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Represents the future responses of a collection of potentially asynchronous requests.
 *
 * @since 0.5.0
 */
public class Fork<T> implements Future<List<T>> {

    private List<PortsFuture<T>> futures = new ArrayList<>();

    Fork() {
        //
    }

    void add(PortsFuture<T> future) {
        futures.add(future);
    }

    /**
     * {@inheritDoc}
     *
     * <p> This particular implementation of get does not throw a CancellationException since
     * Forks are not cancellable.
     *
     * @throws ExecutionException if the receiver terminated unexpectedly
     * @throws java.util.concurrent.CancellationException {@inheritDoc}
     */
    @Override
    public List<T> get() {
        List<T> results = new ArrayList<>();

        for (PortsFuture<T> future : futures) {
            results.add(future.get());
        }

        return results;
    }

    /**
     * {@inheritDoc}
     *
     * <p> This particular implementation of get does not throw a CancellationException since
     * Forks are not cancellable.
     *
     * @throws ExecutionException If the receiver terminated unexpectedly.
     * @throws java.util.concurrent.CancellationException {@inheritDoc}
     */
    @Override
    public List<T> get(long timeout, TimeUnit unit) throws TimeoutException {
        List<T> results = new ArrayList<>();

        long remainingWaitTime = unit.toMillis(timeout);

        for (PortsFuture<T> future : futures) {
            long startTime = System.currentTimeMillis();
            results.add(future.get(remainingWaitTime, TimeUnit.MILLISECONDS));
            remainingWaitTime -= System.currentTimeMillis() - startTime;

            if (remainingWaitTime <= 0L) {
                throw new TimeoutException();
            }
        }

        return results;
    }

    /**
     * Returns a list of {@link Either3} instances providing for each request either the result,
     * a {@link Nothing} (if a timeout occurs), or a {@link Throwable} (if the respective receiver terminated
     * with an exception).
     *
     * <p> <em>This call is blocking.</em>
     */
    public List<Either3<T, Nothing, Throwable>> getEither(long timeout, TimeUnit timeUnit) {
        List<Either3<T, Nothing, Throwable>> results = new ArrayList<>();

        for (PortsFuture<T> future : futures) {
            try {
                T result = future.get(timeout, timeUnit);
                results.add(Either3.a(result));
            } catch (TimeoutException e) {
                results.add(Either3.b(Nothing.INSTANCE));
            } catch (Throwable throwable) {
                results.add(Either3.c(throwable));
            }
        }

        return results;
    }

    /**
     * Returns a list of {@link Either} instances providing for each request either the result or
     * a throwable in case the respective receiver terminated with an exception.
     *
     * <p> <em>This call is blocking.</em>
     */
    public List<Either<T, Throwable>> getEither() {
        List<Either<T, Throwable>> results = new ArrayList<>();

        for (PortsFuture<T> future : futures) {
            try {
                T result = future.get();
                results.add(Either.a(result));
            } catch (Throwable throwable) {
                results.add(Either.b(throwable));
            }
        }

        return results;
    }

    /**
     * Returns a list of {@link Either3} instances providing for each request either (a) the result,
     * (b) a {@link Nothing} in case the result is not yet available, or (c) a {@link Throwable} in case the
     * respective receiver terminated with an exception.
     *
     * <p> <em>This call is non-blocking.</em>
     */
    public List<Either3<T, Nothing, Throwable>> getNowEither() {
        List<Either3<T, Nothing, Throwable>> results = new ArrayList<>();

        for (PortsFuture<T> future : futures) {
            if (future.isDone()) {
                try {
                    T result = future.get();
                    results.add(Either3.a(result));
                } catch (Throwable throwable) {
                    results.add(Either3.c(throwable));
                }
            } else {
                results.add(Either3.b(Nothing.INSTANCE));
            }
        }

        return results;
    }

    /**
     * Instances of Fork are not cancellable, so this method will always return false and do nothing.
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isDone() {
        for (PortsFuture<T> future : futures) {
            if (!future.isDone()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Instances of Fork are not cancellable, so this method will always return false.
     */
    @Override
    public boolean isCancelled() {
        return false;
    }
}
