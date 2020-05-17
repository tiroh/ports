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

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public interface Tuple {

    static <A, B> Pair<A, B> of(A a, B b) {
        return new Pair<>(a, b);
    }

    static <X> PairX<X> ofX(X a, X b) {
        return new PairX<>(a, b);
    }

    static <A, B, C> Triple<A, B, C> of(A a, B b, C c) {
        return new Triple<>(a, b, c);
    }

    static <X> TripleX<X> ofX(X a, X b, X c) {
        return new TripleX<>(a, b, c);
    }

    int arity();
    Object get(int index) throws IndexOutOfBoundsException;
    List<?> toList();
    Object[] toArray();
    Set<?> toOrderedSet();
    void forEach(Consumer<Object> action);
    void forEachNotNull(Consumer<Object> action);
    Tuple reverse();
    Tuple toOptionals();
    Tuple toEithers();
}
