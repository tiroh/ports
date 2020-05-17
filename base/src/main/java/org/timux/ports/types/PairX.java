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
 * A convenience class representing an ordered collection of two values of the same type.
 *
 * @see Pair
 * @see Triple
 * @see TripleX
 * @see Either
 * @see Either3
 *
 * @since 0.5.0
 */
@SuppressWarnings("unchecked")
public class PairX<X> extends Pair<X, X> {

    protected PairX(X a, X b) {
        super(a, b);
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
    public PairX<X> reverse() {
        return new PairX<>(b, a);
    }

    @Override
    public PairX<Optional<X>> toOptionals() {
        return new PairX<>(Optional.ofNullable(a), Optional.ofNullable(b));
    }

    @Override
    public PairX<Either<X, Nothing>> toEithers() {
        return new PairX<>(Either.ofNullable(a), Either.ofNullable(b));
    }

    public void forEachX(Consumer<X> action) {
        action.accept(a);
        action.accept(b);
    }

    public void forEachXNotNull(Consumer<X> action) {
        if (a != null) {
            action.accept(a);
        }

        if (b != null) {
            action.accept(b);
        }
    }

    public <R> PairX<R> map(Function<X, R> mapper) {
        return new PairX<>(mapper.apply(a), mapper.apply(b));
    }
}
