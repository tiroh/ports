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
 * A convenience class representing an ordered collection of two values of the same type.
 *
 * @see PairX
 * @see Triple
 * @see TripleX
 * @see Either
 * @see Either3
 *
 * @since 0.5.0
 */
@SuppressWarnings("unchecked")
public class PairX<X> extends Pair<X, X> {

    public PairX(X a, X b) {
        super(a, b);
    }

    @Override
    public X get(int index) {
        return (X) super.get(index);
    }

    public Tuple<X> asTupleX() {
        return (Tuple<X>) this;
    }
}
