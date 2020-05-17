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

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A convenience class representing an ordered collection of two values of different types.
 *
 * @see PairX
 * @see Triple
 * @see TripleX
 * @see Either
 * @see Either3
 *
 * @since 0.5.0
 */
public class Pair<A, B> implements Tuple {

    protected final A a;
    protected final B b;

    protected Pair(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public A getA() {
        return a;
    }

    public B getB() {
        return b;
    }

    public boolean contains(Object value) {
        if (value == null) {
            return a == null || b == null;
        }

        return value.equals(a) || value.equals(b);
    }

    @Override
    public Object get(int index) {
        if (index < 0 || index >= 2) {
            throw new IndexOutOfBoundsException(index + " is out of bounds (only 0, 1 allowed)");
        }

        return index == 0 ? a : b;
    }

    @Override
    public List<?> toList() {
        List<Object> list = new ArrayList<>();
        list.add(a);
        list.add(b);
        return list;
    }

    @Override
    public Object[] toArray() {
        return new Object[] {a, b};
    }

    @Override
    public Set<?> toOrderedSet() {
        Set<Object> set = new LinkedHashSet<>();
        set.add(a);
        set.add(b);
        return set;
    }

    @Override
    public int getArity() {
        return 2;
    }

    @Override
    public void forEach(Consumer<Object> action) {
        action.accept(a);
        action.accept(b);
    }

    @Override
    public void forEachNotNull(Consumer<Object> action) {
        if (a != null) {
            action.accept(a);
        }

        if (b != null) {
            action.accept(b);
        }
    }

    @Override
    public Pair<B, A> reverse() {
        return new Pair<>(b, a);
    }

    public void on(Consumer<? super A> aConsumer, Consumer<? super B> bConsumer) {
        aConsumer.accept(a);
        bConsumer.accept(b);
    }

    public void onNotNull(Consumer<? super A> aConsumer, Consumer<? super B> bConsumer) {
        if (a != null) {
            aConsumer.accept(a);
        }

        if (b != null) {
            bConsumer.accept(b);
        }
    }

    public <X, Y> Pair<X, Y> map(Function<A, X> mapperA, Function<B, Y> mapperB) {
        return new Pair<>(mapperA.apply(a), mapperB.apply(b));
    }

    public <X> PairX<X> mapX(Function<A, X> mapperA, Function<B, X> mapperB) {
        return new PairX<>(mapperA.apply(a), mapperB.apply(b));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Pair<?, ?> pair = (Pair<?, ?>) o;

        return Objects.equals(a, pair.a) &&
                Objects.equals(b, pair.b);
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b);
    }

    @Override
    public String toString() {
        return "(" + a + ", " + b + ")";
    }
}
