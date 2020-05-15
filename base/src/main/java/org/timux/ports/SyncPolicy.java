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
 * An enum providing options for the way asynchronicity is handled within a {@link Domain}.
 *
 * @since 0.5.0
 */
public enum SyncPolicy {

    /**
     * Specifies that messages shall be processed without any synchronization. Should there be
     * different threads sending messages, the framework will not take any measures of synchronizing
     * them.
     *
     * <p> Take care that the complete domain is
     * thread-safe. Note that it is not enough to ensure that each individual IN port or each
     * individual component is thread-safe as their interplay could still lead to race
     * conditions.
     */
    NONE,

    /**
     * Specifies that message processing is subject to mutual exclusion w.r.t. to
     * individual components.
     *
     * <p> This is the default setting.
     */
    COMPONENT,

    /**
     * Specifies that message processing is subject to mutual exclusion w.r.t. to the
     * complete domain.
     */
    DOMAIN
}
