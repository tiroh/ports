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

/**
 * Creates an {@link Either} instance for two types SUCCESS and FAILURE. The primary use case
 * is as a response type for a {@link Request} that returns either a valid response object or
 * an error status.
 *
 * <p> Use both the {@link SuccessResponse} and the {@link FailureResponse} annotations on a
 * request type in order to indicate the use of this union type.
 *
 * @see SuccessResponse
 * @see FailureResponse
 * @see Either
 * @see Either3
 * @see Nothing
 *
 * @since 0.4.1
 */
public abstract class SuccessOrFailure {

    private SuccessOrFailure() {
        //
    }

    public static <SUCCESS, FAILURE> Either<SUCCESS, FAILURE> success(SUCCESS success) {
        return Either.a(success);
    }

    public static <SUCCESS, FAILURE> Either<SUCCESS, FAILURE> failure(FAILURE failure) {
        return Either.b(failure);
    }
}
