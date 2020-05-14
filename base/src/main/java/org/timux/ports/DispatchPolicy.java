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
     * Specifies that messages shall be dispatched synchronously within the original threads of their
     * respective senders.
     *
     * <p> This is the default setting.
     */
    SYNCHRONOUS,

    /**
     * Specifies that messages shall be dispatched asynchronously within a single separate thread.
     *
     * <p> This setting renders the {@link SyncPolicy} setting effectively irrelevant because with
     * just one thread, there is nothing to synchronize.
     */
    ASYNCHRONOUS,

    /**
     * Specifies that messages shall be dispatched in parallel within an indeterminate number
     * of separate threads. (The number of threads depends on the number of logical cores available to
     * the virtual machine).
     */
    PARALLEL
}
