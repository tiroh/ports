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
 * Use this annotation on a class to indicate that it represents a dynamic component
 * that should not be instantiated automatically by the IoC container.
 *
 * <p>The lifecycle of a dynamic component is managed by the application, not Ports.
 * The application decides when to instantiate a dynamic component. A dynamic component
 * lives until the application removes all references to it, at which point it is
 * eligible for garbage collection.
 *
 * <p>Dynamic components can be instantiated arbitrarily often.
 *
 * <p>Dynamic components cannot receive any requests.
 *
 * @since 0.7.0
 * @see Static
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Dynamic {
}
