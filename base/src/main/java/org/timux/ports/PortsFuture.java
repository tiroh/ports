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

/**
 * Represents a request response that has not yet been computed.
 *
 * <p> Whenever you issue an asynchronous request via {@link Request#callAsync}, you will retrieve
 * an instance of this class. You can access the response via {@link #get()}, {@link #get(long, TimeUnit)},
 * {@link #getNow}, or {@link #getOrElse}.
 *
 * <p> <em>Instances of PortsFuture are not cancellable.</em> Accordingly, both {@link #cancel} and
 * {@link #isCancelled} will always return false.
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
        task = null;

        return result;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws TimeoutException {
        if (hasReturned) {
            return result;
        }

        result = (T) task.waitForResponse(timeout, unit);
        hasReturned = true;
        task = null;

        return result;
    }

    /**
     * Returns either the result or the provided defaultValue, in case the result is not yet available.
     */
    public T getNow(T defaultValue) {
        return task.hasReturned() ? (T) task.waitForResponse() : defaultValue;
    }

    /**
     * Returns an {@link Either} representing either the result (if available) or the provided elseValue.
     */
    public <E> Either<T, E> getOrElse(E elseValue) {
        return task.hasReturned() ? Either.a((T) task.waitForResponse()) : Either.b(elseValue);
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
        return hasReturned ? true : task.hasReturned();
    }

    /**
     * Instances of PortsFuture are not cancellable, so this method will always return false.
     */
    @Override
    public boolean isCancelled() {
        return false;
    }
}
