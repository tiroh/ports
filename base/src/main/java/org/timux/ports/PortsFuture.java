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

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * Represents the future response of a potentially asynchronous request.
 *
 * <p> Whenever you issue an asynchronous request via {@link Request#submit}, you will retrieve
 * an instance of this class. You can access the response via {@link #get()}, {@link #get(long, TimeUnit)},
 * {@link #getNow}, or {@link #getOrElse}.
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
     * @throws ExecutionException if the receiver terminated unexpectedly
     */
    @Override
    public T get(long timeout, TimeUnit unit) throws TimeoutException {
        if (hasReturned) {
            return result;
        }

        result = (T) task.waitForResponse(timeout, unit);
        hasReturned = true;

        return result;
    }

    /**
     * Returns either the result or the provided defaultValue, in case the result is not yet available.
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
     * Returns an {@link Either} representing either the result (if available) or the provided elseValue.
     *
     * @throws ExecutionException If the receiver terminated unexpectedly.
     */
    public <E> Either<T, E> getOrElse(E elseValue) {
        if (hasReturned) {
            return Either.a(result);
        }

        if (task.hasReturned()) {
            result = (T) task.waitForResponse();
            hasReturned = true;
            return Either.a(result);
        }

        return Either.b(elseValue);
    }

    /**
     * Applies the provided mapping function to the result and returns the mapped value. If no result is available,
     * returns the provided 'elseValue'.
     *
     * @throws ExecutionException If the receiver terminated unexpectedly.
     */
    public <R> R mapOrElse(Function<T, R> mapper, R elseValue) {
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
                ? "result='" + (task != null && task.getThrowable() != null ? task.getThrowable().getMessage() : result) + "'}:"
                : "no result available}:");
    }
}
