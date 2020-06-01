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

package org.timux.ports.types;

import java.util.Optional;

/**
 * A type representing a failed computation. When in use, <em>this class should always be instantiated</em>
 * so that the instanceof operator can be used. Null is not an admissible value for this type.
 *
 * @see Success
 * @see Nothing
 * @see Either
 * @see Either3
 *
 * @since 0.5.0
 */
public class Failure {

    public static final Failure INSTANCE = Failure.of("Failure");

    private final String message;
    private final Optional<Throwable> throwable;

    private Failure(String message, Throwable throwable) {
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }

        this.message = message;
        this.throwable = Optional.ofNullable(throwable);
    }

    /**
     * Creates a new Failure instance.
     *
     * @param message Must not be null.
     */
    public static Failure of(String message) {
        return new Failure(message, null);
    }

    /**
     * Creates a new Failure instance.
     *
     * @param throwable May be null.
     */
    public static Failure of(Throwable throwable) {
        return new Failure("", throwable);
    }

    /**
     * Creates a new Failure instance.
     *
     * @param message Must not be null.
     * @param throwable May be null.
     */
    public static Failure of(String message, Throwable throwable) {
        return new Failure(message, throwable);
    }

    public String getMessage() {
        return message;
    }

    public Optional<Throwable> getThrowable() {
        return throwable;
    }

    @Override
    public String toString() {
        return "Failure{" +
                (!message.isEmpty() ? "'" + message + "'" : "") +
                (throwable.isPresent() ? (message.isEmpty() ? throwable : ", " + throwable) : "") +
                "}";
    }
}
