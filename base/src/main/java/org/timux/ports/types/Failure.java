/*
 * Copyright 2018-2021 Tim Rohlfs
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

import org.timux.ports.PortsExecutionException;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Optional;

/**
 * A type representing a failed computation. When in use, <em>this class should always be instantiated</em>
 * so that the instanceof operator can be used. Null is not an admissible value for this type.
 *
 * @see Success
 * @see Empty
 * @see Nothing
 * @see Unknown
 * @see Either
 * @see Either3
 * @since 0.5.0
 */
public final class Failure {

    public static final Failure INSTANCE = Failure.of("Failure");

    private final String message;
    private final Optional<Throwable> throwable;

    private boolean hasAlreadyBeenHandled = false;

    private StackTraceElement[] stackTrace;

    private Failure(String message, Throwable throwable) {
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }

        this.message = message;
        this.throwable = Optional.ofNullable(throwable);

        this.stackTrace = throwable == null
                ? Thread.currentThread().getStackTrace()
                : throwable.getStackTrace();
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
     * @param message   Must not be null.
     * @param throwable May be null.
     */
    public static Failure of(String message, Throwable throwable) {
        return new Failure(message, throwable);
    }

    boolean hasAlreadyBeenHandled() {
        return hasAlreadyBeenHandled;
    }

    void setHasAlreadyBeenHandled() {
        hasAlreadyBeenHandled = true;
    }

    public String getMessage() {
        return message;
    }

    public StackTraceElement[] getStackTrace() {
        return stackTrace;
    }

    public void printStackTrace() {
        printStackTrace(System.err);
    }

    public void printStackTrace(OutputStream outputStream) {
        try (PrintWriter writer = new PrintWriter(outputStream)) {
            printStackTrace(writer);
        }
    }

    public void printStackTrace(PrintWriter printWriter) {
        synchronized (printWriter) {
            printWriter.println(this);

            boolean preambleHasBeenSkipped = false;

            for (int lineNo = 0; lineNo < stackTrace.length; lineNo++) {
                StackTraceElement element = stackTrace[lineNo];

                if (!preambleHasBeenSkipped
                        && (element.getClassName().equals(getClass().getName()))
                        || element.getClassName().equals(Thread.class.getName())) {
                    continue;
                } else {
                    preambleHasBeenSkipped = true;
                }

                printWriter.print("    at ");
                printWriter.println(element);
            }
        }
    }

    /**
     * @see #getFirstNonPortsThrowable()
     * @see #getRootCause()
     */
    public Optional<Throwable> getThrowable() {
        return throwable;
    }

    /**
     * Returns an {@link Optional} containing the first {@link Throwable} that is
     * not a {@link PortsExecutionException}.
     * If such a Throwable does not exist, an empty Optional is returned.
     *
     * @see #getRootCause()
     * @see #getThrowable()
     */
    public Optional<Throwable> getFirstNonPortsThrowable() {
        Optional<Throwable> t = throwable;

        while (t.isPresent()) {
            Throwable tt = t.get();

            // We do NOT want 'instanceof' here because of inheritance!
            if (!(tt.getClass() == PortsExecutionException.class)) {
                return t;
            }

            t = Optional.ofNullable(t.get().getCause());
        }

        return Optional.empty();
    }

    /**
     * Returns an {@link Optional} containing the last {@link Throwable} in the exception chain,
     * if it exists; or an empty Optional otherwise.
     *
     * @see #getFirstNonPortsThrowable()
     * @see #getThrowable()
     */
    public Optional<Throwable> getRootCause() {
        Optional<Throwable> t = throwable;

        for (; ; ) {
            if (!t.isPresent()) {
                return Optional.empty();
            }

            Throwable tt = t.get().getCause();

            if (tt == null) {
                return t;
            }

            t = Optional.of(tt);
        }
    }

    @Override
    public String toString() {
        return "Failure{" +
                (!message.isEmpty() ? "'" + message + "'" : "") +
                (throwable.isPresent() ? (message.isEmpty() ? throwable.get() : ", " + throwable.get()) : "") +
                "}";
    }
}
