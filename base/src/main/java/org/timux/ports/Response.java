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

import org.timux.ports.types.Either;
import org.timux.ports.types.Either3;

import java.lang.annotation.*;

/**
 * Use this annotation to declare a response type for a {@link Request} type.
 * This declaration will serve as a hint for both the application developer
 * and the Ports Framework.
 * <p>
 * Starting with Ports 0.4.1, you may use up to 3 of these annotations at once on a
 * request type. This indicates the use of a union type as a response type. In this
 * case, your requests must return an {@link Either} (2 annotations) or
 * an {@link Either3} (3 annotations).
 *
 * @see Either
 * @see Either3
 *
 * @author Tim Rohlfs
 * @since 0.4.0
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Responses.class)
public @interface Response {

    Class<?> value();
}
