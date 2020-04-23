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

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A union type for two types SUCCESS and FAILURE. The primary use case is as a response
 * type for a {@link Request} that returns either a valid response object or an error status.
 * <p>
 * Use both the {@link SuccessResponse} and the {@link FailureResponse} annotations on a
 * request type in order to indicate the use of this union type.
 *
 * @see SuccessResponse
 * @see FailureResponse
 * @see Either
 * @see Either3
 *
 * @param <SUCCESS> The type of the success object.
 * @param <FAILURE> The type of the failure object.
 *
 * @since 0.4.1
 */
public abstract class SuccessOrFailure<SUCCESS, FAILURE> {

    private SuccessOrFailure() {
        //
    }

    /**
     * Maps the constituents of this union to R.
     */
    public abstract <R> R map(Function<? super SUCCESS, ? extends R> successFn, Function<? super FAILURE, ? extends R> failureFn);

    /**
     * Executes the provided actions on the constituents of this union.
     */
    public abstract void do_(Consumer<? super SUCCESS> successConsumer, Consumer<? super FAILURE> failureConsumer);

    /**
     * Returns the SUCCESS constituent of this union in the form of an {@link Optional}.
     */
    public Optional<SUCCESS> getSuccess() {
        return map(Optional::ofNullable, failure -> Optional.empty());
    }

    /**
     * Returns the FAILURE constituent of this union in the form of an {@link Optional}.
     */
    public Optional<FAILURE> getFailure() {
        return map(success -> Optional.empty(), Optional::ofNullable);
    }

    @Override
    public String toString() {
        return map(String::valueOf, String::valueOf);
    }

    /**
     * Creates an instance of this union that contains a SUCCESS (non-null).
     */
    public static <SUCCESS, FAILURE> SuccessOrFailure<SUCCESS, FAILURE> success(SUCCESS success) {
        if (success == null) {
            throw new IllegalArgumentException("argument must not be null");
        }

        return new SuccessOrFailure<SUCCESS, FAILURE>() {

            @Override
            public <R> R map(Function<? super SUCCESS, ? extends R> successFn, Function<? super FAILURE, ? extends R> failureFn) {
                return successFn.apply(success);
            }

            @Override
            public void do_(Consumer<? super SUCCESS> successConsumer, Consumer<? super FAILURE> failureConsumer) {
                successConsumer.accept(success);
            }
        };
    }

    /**
     * Creates an instance of this union that contains a FAILURE (non-null).
     */
    public static <SUCCESS, FAILURE> SuccessOrFailure<SUCCESS, FAILURE> failure(FAILURE failure) {
        if (failure == null) {
            throw new IllegalArgumentException("argument must not be null");
        }

        return new SuccessOrFailure<SUCCESS, FAILURE>() {

            @Override
            public <R> R map(Function<? super SUCCESS, ? extends R> successFn, Function<? super FAILURE, ? extends R> failureFn) {
                return failureFn.apply(failure);
            }

            @Override
            public void do_(Consumer<? super SUCCESS> successConsumer, Consumer<? super FAILURE> failureConsumer) {
                failureConsumer.accept(failure);
            }
        };
    }
}
