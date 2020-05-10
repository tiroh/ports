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

import java.util.List;

/**
 * An exception that is thrown when the receiver of a request terminates
 * with an exception instead of delivering a response.
 *
 * <p> This exception differs from Java's {@link java.util.concurrent.ExecutionException}
 * in that it inherits from {@link RuntimeException} instead of
 * {@link Exception}.
 *
 * @see Request#call
 * @see Request#submit
 * @see Request#fork(List)
 *
 * @since 0.5.0
 */
public class ExecutionException extends RuntimeException {

    ExecutionException(Throwable t) {
        super(t);
    }
}
