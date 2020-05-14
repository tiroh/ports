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

import org.timux.ports.FailureResponse;
import org.timux.ports.SuccessResponse;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A convenience class that makes it easier to work with commands that are either successful or
 * return a failure report.
 *
 * @see SuccessResponse
 * @see FailureResponse
 * @see Either
 * @see Either3
 * @see Nothing
 * @see Success
 * @see Failure
 *
 * @since 0.4.1
 */
public class SuccessOrFailure<DETAILS> {

    private final Either<Success, Pair<Failure, DETAILS>> either;

    private SuccessOrFailure(Either<Success, Pair<Failure, DETAILS>> either) {
        this.either = either;
    }

    public static <FAILURE> SuccessOrFailure<FAILURE> success() {
        return new SuccessOrFailure<>(Either.a(Success.INSTANCE));
    }

    public static <DETAILS> SuccessOrFailure<DETAILS> failure(Failure failure, DETAILS details) {
        return new SuccessOrFailure<>(Either.b(new Pair<>(failure, details)));
    }

    public SuccessOrFailure<DETAILS> on(Consumer<? super Success> success, Consumer<? super Pair<Failure, DETAILS>> failure) {
        either.on(success, failure);
        return this;
    }

    public <R> R map(Function<? super Success, R> success, Function<? super Pair<Failure, DETAILS>, R> failure) {
        return either.map(success, failure);
    }

    public SuccessOrFailure<DETAILS> onSuccess(Runnable runnable) {
        either.onA(success -> runnable.run());
        return this;
    }

    public SuccessOrFailure<DETAILS> onFailure(Consumer<? super Pair<Failure, DETAILS>> failure) {
        either.onB(failure);
        return this;
    }

    public boolean isSuccess() {
        return either.getA().isPresent();
    }

    public Optional<Pair<Failure, DETAILS>> getFailure() {
        return either.getB();
    }
}
