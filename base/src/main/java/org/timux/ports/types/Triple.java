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
 * A convenience class representing an ordered collection of three values of different types.
 *
 * @see TripleX
 * @see Pair
 * @see PairX
 * @see Either
 * @see Either3
 *
 * @since 0.5.0
 */
public class Triple<A, B, C> implements Tuple {

    protected final A a;
    protected final B b;
    protected final C c;

    protected Triple(A a, B b, C c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public A getA() {
        return a;
    }

    public B getB() {
        return b;
    }

    public C getC() {
        return c;
    }

    public boolean contains(Object value) {
        if (value == null) {
            return a == null || b == null || c == null;
        }

        return value.equals(a) || value.equals(b) || value.equals(c);
    }

    public boolean containsDistinct(Pair<?, ?> pair) {
        return containsDistinct(pair.getA(), pair.getB());
    }

    public boolean containsDistinct(Object x, Object y) {
        if (x == null && y == null) {
            if (a == null) {
                return b == null || c == null;
            }

            return b == null && c == null;
        }

        if (x == null) {
            if (a == null) {
                return y.equals(b) || y.equals(c);
            }

            if (b == null) {
                return y.equals(a) || y.equals(c);
            }

            return c == null && (y.equals(a) || y.equals(b));
        }

        if (y == null) {
            if (a == null) {
                return x.equals(b) || x.equals(c);
            }

            if (b == null) {
                return x.equals(a) || x.equals(c);
            }

            return c == null && (x.equals(a) || x.equals(b));
        }

        if (x.equals(a)) {
            return y.equals(b) || y.equals(c);
        }

        if (x.equals(b)) {
            return y.equals(a) || y.equals(c);
        }

        return x.equals(c) && (y.equals(a) || y.equals(b));
    }

    public boolean containsAny(Pair<?, ?> pair) {
        return contains(pair.getA()) || contains(pair.getB());
    }

    public boolean containsAny(Triple<?, ?, ?> triple) {
        return contains(triple.getA()) || contains(triple.getB()) || contains(triple.getC());
    }

    public boolean containsAny(Object x, Object y) {
        return contains(x) || contains(y);
    }

    public boolean containsAny(Object x, Object y, Object z) {
        return contains(x) || contains(y) || contains(z);
    }

    @Override
    public Object get(int index) {
        if (index < 0 || index >= 3) {
            throw new IndexOutOfBoundsException(index + " is out of bounds (only 0, 1, 2 allowed)");
        }

        return index == 0 ? a : (index == 1 ? b : c);
    }

    @Override
    public List<?> toList() {
        List<Object> list = new ArrayList<>();
        list.add(a);
        list.add(b);
        list.add(c);
        return list;
    }

    @Override
    public Object[] toArray() {
        return new Object[] {a, b, c};
    }

    @Override
    public Set<?> toOrderedSet() {
        Set<Object> set = new LinkedHashSet<>();
        set.add(a);
        set.add(b);
        set.add(c);
        return set;
    }

    @Override
    public int getArity() {
        return 3;
    }

    @Override
    public void forEach(Consumer<Object> action) {
        action.accept(a);
        action.accept(b);
        action.accept(c);
    }

    @Override
    public void forEachNotNull(Consumer<Object> action) {
        if (a != null) {
            action.accept(a);
        }

        if (b != null) {
            action.accept(b);
        }

        if (c != null) {
            action.accept(b);
        }
    }

    @Override
    public Triple<C, B, A> reverse() {
        return new Triple<>(c, b, a);
    }

    @Override
    public Triple<Optional<A>, Optional<B>, Optional<C>> toOptionals() {
        return new Triple<>(Optional.ofNullable(a), Optional.ofNullable(b), Optional.ofNullable(c));
    }

    @Override
    public Triple<Either<A, Nothing>, Either<B, Nothing>, Either<C, Nothing>> toEithers() {
        return new Triple<>(Either.ofNullable(a), Either.ofNullable(b), Either.ofNullable(c));
    }

    public void on(Consumer<? super A> aConsumer, Consumer<? super B> bConsumer, Consumer<? super C> cConsumer) {
        aConsumer.accept(a);
        bConsumer.accept(b);
        cConsumer.accept(c);
    }

    public void onNotNull(Consumer<? super A> aConsumer, Consumer<? super B> bConsumer, Consumer<? super C> cConsumer) {
        if (a != null) {
            aConsumer.accept(a);
        }

        if (b != null) {
            bConsumer.accept(b);
        }

        if (c != null) {
            cConsumer.accept(c);
        }
    }

    public <X, Y, Z> Triple<X, Y, Z> map(Function<A, X> mapperA, Function<B, Y> mapperB, Function<C, Z> mapperC) {
        return new Triple<>(mapperA.apply(a), mapperB.apply(b), mapperC.apply(c));
    }

    public <X> TripleX<X> mapX(Function<A, X> mapperA, Function<B, X> mapperB, Function<C, X> mapperC) {
        return new TripleX<>(mapperA.apply(a), mapperB.apply(b), mapperC.apply(c));
    }

    public Pair<A, B> pairAB() {
        return new Pair<>(a, b);
    }

    public Pair<A, C> pairAC() {
        return new Pair<>(a, c);
    }

    public Pair<B, C> pairBC() {
        return new Pair<>(b, c);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Triple<?, ?, ?> triple = (Triple<?, ?, ?>) o;

        return Objects.equals(a, triple.a) &&
                Objects.equals(b, triple.b) &&
                Objects.equals(c, triple.c);
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b, c);
    }

    @Override
    public String toString() {
        return "(" + a + ", " + b + ", " + c + ")";
    }
}
