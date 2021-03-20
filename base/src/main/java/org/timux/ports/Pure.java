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

package org.timux.ports;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation on a request type in order to indicate that (a) the corresponding call
 * is guaranteed to always return the same value for the same input and (b) the request does not
 * cause significant side effects.
 *
 * <p> Ports may decide to cache the responses of pure requests in order to increase
 * performance. However, Ports will never cache failures. Caching can be disabled by
 * setting this annotation's {@link Pure#cache()} property to false.
 *
 * <p> Be aware that you must not use this annotation on request types whose handlers
 * perform output or transform the system state in another significant way.
 *
 * <p> A request type that is both pure and stateful must implement both {@link #equals(Object)}
 * and {@link #hashCode()} if caching is enabled. It is recommended to use your IDE's code
 * generation for that in order to avoid subtle errors.
 *
 * @author Tim Rohlfs
 * @since 0.5.12
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Pure {

    /**
     * Set this property to false in order to disable caching.
     * By default, caching is enabled.
     */
    boolean cache() default true;
}
