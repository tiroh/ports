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

/**
 * A convenience class for storing arbitrary values. This is mainly for testing with protocols.
 * Protocols are based on lambdas. Java's lambdas cannot form closures with non-final data.
 * So, a container like this class can be used to provide a final reference to non-final data.
 *
 * <p> The 'value' member is public because the whole point of this class is to make data modifiable.
 * This class only exists for technical reasons, not because of design reasons.
 *
 * @since 0.5.0
 */
public class Container<T> {

    public T value;

    private Container(T value) {
        this.value = value;
    }

    public static <T> Container<T> of(T value) {
        return new Container<>(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
