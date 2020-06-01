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
import org.timux.ports.types.Nothing;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents the future response of a potentially asynchronous request.
 *
 * <p> Whenever you issue an asynchronous request via {@link Request#submit}, you will retrieve
 * an instance of this class. You can access the response via {@link #get()}, {@link #get(long, TimeUnit)},
 * {@link #getNow}, or {@link #getEither}.
 *
 * <p> <em>Instances of PortsFuture are not cancellable.</em> Accordingly, both {@link #cancel} and
 * {@link #isCancelled} always return false.
 *
 * @param <T> The type of the expected response.
 *
 * @author Tim Rohlfs
 * @since 0.5.0
 */
@SuppressWarnings("unchecked")
public class PortsFuture<T> implements Future<T> {

    private Task task;
    private T result;
    private boolean hasReturned;

    PortsFuture(T result) {
        this.result = result;
        hasReturned = true;
    }

    PortsFuture(Task task) {
        this.task = task;
    }

    /**
     * {@inheritDoc}
     *
     * <p> This particular implementation of get does not throw a CancellationException since
     * PortsFutures are not cancellable.
     *
     * @throws ExecutionException if the receiver terminated unexpectedly
     * @throws java.util.concurrent.CancellationException {@inheritDoc}
     */
    @Override
    public T get() {
        if (hasReturned) {
            return result;
        }

        result = (T) task.waitForResponse();
        hasReturned = true;

        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p> This particular implementation of get does not throw a CancellationException since
     * PortsFutures are not cancellable.
     *
     * @throws ExecutionException if the receiver terminated unexpectedly
     * @throws java.util.concurrent.CancellationException {@inheritDoc}
     */
    @Override
    public T get(long timeout, TimeUnit unit) throws TimeoutException {
        if (!hasReturned) {
            result = (T) task.waitForResponse(timeout, unit);
            hasReturned = true;
        }

        return result;
    }

    /**
     * Returns an {@link Either} providing either the result or a {@link Throwable} in case the
     * respective receiver terminated with an exception.
     *
     * <p> <em>This call is blocking.</em>
     */
    public Either<T, Throwable> getEither() {
        try {
            return Either.a(get());
        } catch (Exception e) {
            return Either.b(e);
        }
    }

    /**
     * Returns an {@link Either3} providing either the result, a {@link Nothing} (if a timeout occurs),
     * or a {@link Throwable} (if the respective receiver terminated with an exception).
     *
     * <p> <em>This call is blocking.</em>
     */
    public Either3<T, Nothing, Throwable> getEither(long timeout, TimeUnit timeUnit) {
        try {
            return Either3.a(get(timeout, timeUnit));
        } catch (TimeoutException e) {
            return Either3.b(Nothing.INSTANCE);
        } catch (Exception e) {
            return Either3.c(e);
        }
    }

    /**
     * Returns either the result or the provided 'defaultValue', in case the result is not yet available.
     *
     * <p> <em>This call is non-blocking.</em>
     *
     * @throws ExecutionException If the receiver terminated unexpectedly.
     */
    public T getNow(T defaultValue) {
        if (hasReturned) {
            return result;
        }

        if (task.hasReturned()) {
            result = (T) task.waitForResponse();
            hasReturned = true;
            return result;
        }

        return defaultValue;
    }

    /**
     * Returns an {@link Either3} representing either the result (if available), the provided 'elseValue', or
     * a {@link Throwable} (if the respective receiver terminated with an exception).
     *
     * <p> <em>This call is non-blocking.</em>
     */
    public <E> Either3<T, E, Throwable> getEither(E elseValue) {
        if (hasReturned) {
            return Either3.a(result);
        }

        if (task.hasReturned()) {
            try {
                result = (T) task.waitForResponse();
                hasReturned = true;
                return Either3.a(result);
            } catch (Exception e) {
                return Either3.c(e);
            }
        }

        return Either3.b(elseValue);
    }

    /**
     * Applies the provided mapping function to the result and returns the mapped value. If no result is available,
     * returns the provided 'elseValue'.
     *
     * <p> <em>This call is non-blocking.</em>
     *
     * @throws ExecutionException If the receiver terminated unexpectedly.
     */
    public <R> R map(Function<T, R> mapper, R elseValue) {
        if (hasReturned) {
            return mapper.apply(result);
        }

        if (task.hasReturned()) {
            result = (T) task.waitForResponse();
            hasReturned = true;
            return mapper.apply(result);
        }

        return elseValue;
    }

    /**
     * Returns an {@link Either} containing either:
     *
     * <ol>
     * <li>the result transformed by the provided 'mapper' function (if the result is available),</li>
     * <li>the provided 'elseValue' (if no result is available),</li>
     * <li>a {@link Throwable} (if the receiver terminated with an exception).</li>
     * </ol>
     *
     * <p> <em>This call is non-blocking.</em>
     */
    public <R> Either<R, Throwable> mapEither(Function<T, R> mapper, R elseValue) {
        try {
            return Either.a(map(mapper, elseValue));
        } catch (Exception e) {
            return Either.b(e);
        }
    }

    /**
     * A method that makes handling chains of requests easier. It works in conjunction with
     * {@link Either#andThenR}.
     *
     * <p> It maps the result, if it exists, to another {@link PortsFuture}, or returns a {@link Throwable}
     * if the receiver terminated with an exception.
     *
     * <p> <em>This call is blocking.</em>
     *
     * @see Either#andThenR
     */
    public <O, R extends PortsFuture<O>> Either<O, Throwable> andThenR(Function<T, R> fn) {
        return getEither().andThenR(fn);
    }

    /**
     * If the receiver terminated with an exception, this method maps this exception to R. Otherwise, the result
     * is returned.
     *
     * <p> <em>This call is blocking.</em>
     * 
     * @see #orElseDo
     */
    public <R> Either<T, R> orElse(Function<Throwable, R> fn) {
        return getEither().orElse(fn);
    }

    /**
     * If the receiver terminated with an exception, this method executes the provided consumer. Otherwise,
     * nothing happens.
     *
     * <p> <em>This call is blocking.</em>
     *
     * @see #orElse
     */
    public Either<T, Throwable> orElseDo(Consumer<Throwable> consumer) {
        return getEither().orElseDo(consumer);
    }

    /**
     * Instances of PortsFuture are not cancellable, so this method will always return false and do nothing.
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isDone() {
        return hasReturned || task.hasReturned();
    }

    /**
     * Instances of PortsFuture are not cancellable, so this method will always return false.
     */
    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public String toString() {
        return "PortsFuture{"
                + (isDone()
                ? "result='" + (task != null && task.getThrowable() != null ? task.getThrowable().getMessage() : task.getResponse()) + "'}"
                : "no result available}");
    }
}
