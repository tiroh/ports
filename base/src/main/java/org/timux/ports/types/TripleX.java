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
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A convenience class representing an ordered collection of three values of the same type.
 *
 * @see Triple
 * @see Pair
 * @see PairX
 * @see Either
 * @see Either3
 *
 * @since 0.5.0
 */
@SuppressWarnings("unchecked")
public class TripleX<X> extends Triple<X, X, X> {

    protected TripleX(X a, X b, X c) {
        super(a, b, c);
    }

    @Override
    public X get(int index) {
        return (X) super.get(index);
    }

    @Override
    public List<X> toList() {
        return (List<X>) super.toList();
    }

    @Override
    public X[] toArray() {
        return (X[]) super.toArray();
    }

    @Override
    public Set<X> toOrderedSet() {
        return (Set<X>) super.toOrderedSet();
    }

    @Override
    public TripleX<X> reverse() {
        return new TripleX<>(c, b, a);
    }

    @Override
    public TripleX<Optional<X>> toOptionals() {
        return new TripleX<>(Optional.ofNullable(a), Optional.ofNullable(b), Optional.ofNullable(c));
    }

    @Override
    public TripleX<Either<X, Nothing>> toEithers() {
        return new TripleX<>(Either.ofNullable(a), Either.ofNullable(b), Either.ofNullable(c));
    }

    public void forEachX(Consumer<X> action) {
        action.accept(a);
        action.accept(b);
        action.accept(c);
    }

    public void forEachXNotNull(Consumer<X> action) {
        if (a != null) {
            action.accept(a);
        }

        if (b != null) {
            action.accept(b);
        }

        if (c != null) {
            action.accept(c);
        }
    }

    public <R> TripleX<R> map(Function<X, R> mapper) {
        return new TripleX<>(mapper.apply(a), mapper.apply(b), mapper.apply(c));
    }

    @Override
    public PairX<X> pairAB() {
        return new PairX<>(a, b);
    }

    @Override
    public PairX<X> pairAC() {
        return new PairX<>(a, c);
    }

    @Override
    public PairX<X> pairBC() {
        return new PairX<>(b, c);
    }
}
