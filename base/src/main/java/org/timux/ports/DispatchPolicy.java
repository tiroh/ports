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
 * An enum providing options for the way message dispatching is handled within a {@link Domain}.
 *
 * @since 0.5.0
 */
public enum DispatchPolicy {

    /**
     * Specifies that messages shall be dispatched within the original threads of their
     * respective senders. This implies synchronous execution.
     *
     * <p> This is the default setting.
     */
    SAME_THREAD,

    /**
     * Specifies that messages shall be dispatched asynchronously within a separate thread.
     */
    ASYNCHRONOUS,

    /**
     * Specifies that messages shall be dispatched in parallel within an indeterminate number
     * of separate threads.
     */
    PARALLEL
}
