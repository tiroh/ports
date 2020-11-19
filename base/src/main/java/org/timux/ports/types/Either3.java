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

import org.timux.ports.PortsFuture;
import org.timux.ports.Request;
import org.timux.ports.Response;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A union type for three types A, B, and C. The primary use case is as a response type for a
 * {@link Request} that may return different kinds of data, depending on the situation.
 *
 * <p> Use multiple {@link Response} annotations on a request type in order to indicate the
 * use of this union type.
 *
 * @see Either
 * @see Empty
 * @see Nothing
 * @see Unknown
 * @see Pair
 * @see Triple
 *
 * @param <A> The first type.
 * @param <B> The second type.
*  @param <C> The third type.
 *
 * @since 0.4.1
 */
public abstract class Either3<A, B, C> {

    private Either3() {
        //
    }

    public static <T, U> Either3<Success, T, U> success() {
        return Either3.a(Success.INSTANCE);
    }

    public static <T, U> Either3<T, U, Failure> failure() {
        return Either3.c(Failure.INSTANCE);
    }

    public static <T, U> Either3<T, U, Failure> failure(String message) {
        return Either3.c(Failure.of(message));
    }

    public static <T, U> Either3<T, U, Failure> failure(Throwable throwable) {
        return Either3.c(Failure.of(throwable));
    }

    public static <T, U> Either3<T, U, Failure> failure(String message, Throwable throwable) {
        return Either3.c(Failure.of(message, throwable));
    }

    public static <T, U, V> Either3<T, U, Failure> failure(Either<V, Failure> either) {
        return either.map(v -> Either3.c(Failure.INSTANCE), Either3::c);
    }

    public static <T, U, V, W> Either3<T, U, Failure> failure(Either3<V, W, Failure> either3) {
        return either3.map(v -> Either3.c(Failure.INSTANCE), w -> Either3.c(Failure.INSTANCE), Either3::c);
    }

    public static <T, U> Either3<T, Nothing, U> nothing() {
        return Either3.b(Nothing.INSTANCE);
    }

    public static <T, U> Either3<T, Unknown, U> unknown() {
        return Either3.b(Unknown.INSTANCE);
    }

    /**
     * Returns an {@link Either3} containing either the return value of the {@code supplier},
     * {@link Nothing} in case the {@code supplier} returns null,
     * or {@link Failure} in case the {@code supplier} throws an exception.
     *
     * <p>If you want to handle the case that the {@code supplier} returns null in combination with
     * the exception case, use {@link Either#valueOrFailure} instead.
     *
     * <p>If you want to ignore the return value of the {@code supplier}, use {@link Either#successOrFailure(Supplier)}
     * instead.
     */
    public static <T> Either3<T, Nothing, Failure> valueOrNothingOrFailure(Supplier<T> supplier) {
        try {
            T t = supplier.get();

            if (t == null) {
                return Either3.b(Nothing.INSTANCE);
            } else {
                return Either3.a(t);
            }
        } catch (Exception e) {
            return Either3.c(Failure.of(e));
        }
    }

    /**
     * Maps the constituents of this union to R.
     */
    public abstract <R> R map(Function<? super A, ? extends R> aFn, Function<? super B, ? extends R> bFn, Function<? super C, ? extends R> cFn);

    /**
     * Maps the A constituent, if it exists, to R.
     */
    public abstract <R> Either3<R, B, C> mapA(Function<? super A, ? extends R> aFn);

    /**
     * Maps the B constituent, if it exists, to R.
     */
    public abstract <R> Either3<A, R, C> mapB(Function<? super B, ? extends R> bFn);

    /**
     * Maps the C constituent, if it exists, to R.
     */
    public abstract <R> Either3<A, B, R> mapC(Function<? super C, ? extends R> cFn);

    /**
     * Maps the A constituent, if it exists, to R, wrapped into a new {@link Either3}.
     *
     * @see #andThen
     * @see #andThenR
     */
    public abstract <R> Either3<R, B, C> andThenE(Function<? super A, ? extends R> aFn);

    /**
     * Maps the A constituent, if it exists, to R, which must be an {@link Either3} that has the
     * same B and C types like this {@link Either3}.
     */
    public abstract <R extends Either3<?, B, C>> R andThen(Function<? super A, ? extends R> aFn);

    /**
     * Applies the provided consumer to the A constituent, if it exists, or does nothing otherwise.
     */
    public abstract Either3<A, B, C> andThenDo(Consumer<? super A> aC);

    /**
     * A version of {@link #andThen} that supports working with requests. With this method (and together with
     * {@link PortsFuture#andThenE}) you can build chains of requests.
     *
     * <p> It maps the A constituent, if it exists, to (a {@link PortsFuture} R, or returns the B constituent
     * otherwise. In this context, the A constituent is the result of a preceding request.
     *
     * @see PortsFuture#andThenE
     * @see #andThen
     * @see #andThenE
     * @see #orElse
     * @see #orElseDo
     * @see #finallyDo
     */
    public abstract <R> Either<R, Failure> andThenR(Function<? super A, ? extends PortsFuture<R>> aFn);

    /**
     * Maps the C constituent, if it exists, to R.
     */
    public abstract <R> Either3<A, B, R> orElse(Function<? super C, R> cFn);

    /**
     * Applies the provided consumer to the C constituent, if it exists, or does nothing otherwise.
     */
    public abstract Either3<A, B, C> orElseDo(Consumer<? super C> cC);

    /**
     * If the C constituent is a {@link Failure}, this method applies the provided
     * consumer to that failure only if it has not already been handled by
     * another call of {@link #orElseDoOnce}, {@link #orElse}, or {@link #orElseDo}.
     * Otherwise, this method behaves exactly like {@link #orElseDo}.
     */
    public abstract Either3<A, B, C> orElseDoOnce(Consumer<? super C> cC);

    /**
     * Executes the provided actions on the constituents of this union.
     */
    public abstract Either3<A, B, C> on(Consumer<? super A> aC, Consumer<? super B> bC, Consumer<? super C> cC);

    /**
     * Executes the provided action on the A constituent, if it exists, and returns this union.
     */
    public abstract Either3<A, B, C> onA(Consumer<? super A> aC);

    /**
     * Executes the provided action on the B constituent, if it exists, and returns this union.
     */
    public abstract Either3<A, B, C> onB(Consumer<? super B> bC);

    /**
     * Executes the provided action on the C constituent, if it exists, and returns this union.
     */
    public abstract Either3<A, B, C> onC(Consumer<? super C> cC);

    /**
     * Returns a {@link Triple} that represents the current state of this union, indicating a missing
     * value with null.
     */
    public abstract Triple<A, B, C> toTriple();

    /**
     * Returns a {@link Triple} that represents the current state of this union, indicating a missing
     * value with an empty {@link Optional}.
     */
    public abstract Triple<Optional<A>, Optional<B>, Optional<C>> toTripleOfOptionals();

    /**
     * Returns the A constituent of this union in the form of an {@link Optional}.
     */
    public Optional<A> getA() {
        return map(Optional::ofNullable, b -> Optional.empty(), c -> Optional.empty());
    }

    /**
     * Returns the B constituent of this union in the form of an {@link Optional}.
     */
    public Optional<B> getB() {
        return map(a -> Optional.empty(), Optional::ofNullable, c -> Optional.empty());
    }

    /**
     * Returns the C constituent of this union in the form of an {@link Optional}.
     */
    public Optional<C> getC() {
        return map(a -> Optional.empty(), b -> Optional.empty(), Optional::ofNullable);
    }

    /**
     * Returns the A constituent of this union or throws a {@link NoSuchElementException} if it
     * doesn't exist.
     */
    public abstract A getAOrThrow() throws NoSuchElementException;

    /**
     * Returns the B constituent of this union or throws a {@link NoSuchElementException} if it
     * doesn't exist.
     */
    public abstract B getBOrThrow() throws NoSuchElementException;

    /**
     * Returns the C constituent of this union or throws a {@link NoSuchElementException} if it
     * doesn't exist.
     */
    public abstract C getCOrThrow() throws NoSuchElementException;

    /**
     * Returns true if this union represents an instance of {@link Success},
     * and false otherwise.
     */
    public boolean isSuccess() {
        return map(
                a -> a.getClass() == Success.class,
                b -> b.getClass() == Success.class,
                c -> c.getClass() == Success.class);
    }

    /**
     * Returns true if this union represents an instance of {@link Failure},
     * and false otherwise.
     */
    public boolean isFailure() {
        return map(
                a -> a.getClass() == Failure.class,
                b -> b.getClass() == Failure.class,
                c -> c.getClass() == Failure.class);
    }

    /**
     * Returns true if this union represents an instance of {@link Nothing},
     * and false otherwise.
     */
    public boolean isNothing() {
        return map(
                a -> a.getClass() == Nothing.class,
                b -> b.getClass() == Nothing.class,
                c -> c.getClass() == Nothing.class);
    }

    /**
     * Returns true if this union represents an instance of {@link Unknown},
     * and false otherwise.
     */
    public boolean isUnknown() {
        return map(
                a -> a.getClass() == Unknown.class,
                b -> b.getClass() == Unknown.class,
                c -> c.getClass() == Unknown.class);
    }

    /**
     * Executes the provided runnable and returns this union.
     */
    public Either3<A, B, C> finallyDo(Runnable runnable) {
        runnable.run();
        return this;
    }

    @Override
    public String toString() {
        return map(String::valueOf, String::valueOf, String::valueOf);
    }

    /**
     * Creates an instance of this union that contains an A (non-null).
     */
    public static <A, B, C> Either3<A, B, C> a(A a) throws IllegalArgumentException {
        if (a == null) {
            throw new IllegalArgumentException("argument must not be null");
        }

        return new Either3<A, B, C>() {

            @Override
            public <R> R map(Function<? super A, ? extends R> aFn, Function<? super B, ? extends R> bFn, Function<? super C, ? extends R> cFn) {
                return aFn.apply(a);
            }

            @Override
            public <R> Either3<R, B, C> mapA(Function<? super A, ? extends R> aFn) {
                return Either3.a(aFn.apply(a));
            }

            @Override
            @SuppressWarnings("unchecked")
            public <R> Either3<A, R, C> mapB(Function<? super B, ? extends R> bFn) {
                return (Either3<A, R, C>) this;
            }

            @Override
            @SuppressWarnings("unchecked")
            public <R> Either3<A, B, R> mapC(Function<? super C, ? extends R> cFn) {
                return (Either3<A, B, R>) this;
            }

            @Override
            public <R> Either3<R, B, C> andThenE(Function<? super A, ? extends R> aFn) {
                return Either3.a(aFn.apply(a));
            }

            @Override
            public <R extends Either3<?, B, C>> R andThen(Function<? super A, ? extends R> aFn) {
                return aFn.apply(a);
            }

            @Override
            public Either3<A, B, C> andThenDo(Consumer<? super A> aC) {
                aC.accept(a);
                return this;
            }

            @Override
            public <R> Either<R, Failure> andThenR(Function<? super A, ? extends PortsFuture<R>> aFn) {
                return aFn.apply(a).getE();
            }

            @Override
            public <R> Either3<A, B, R> orElse(Function<? super C, R> cFn) {
                return Either3.a(a);
            }

            @Override
            public Either3<A, B, C> orElseDo(Consumer<? super C> cC) {
                return this;
            }

            @Override
            public Either3<A, B, C> orElseDoOnce(Consumer<? super C> cC) {
                return this;
            }

            @Override
            public Either3<A, B, C> on(Consumer<? super A> aC, Consumer<? super B> bC, Consumer<? super C> cC) {
                aC.accept(a);
                return this;
            }

            @Override
            public Either3<A, B, C> onA(Consumer<? super A> aC) {
                aC.accept(a);
                return this;
            }

            @Override
            public Either3<A, B, C> onB(Consumer<? super B> bC) {
                return this;
            }

            @Override
            public Either3<A, B, C> onC(Consumer<? super C> cC) {
                return this;
            }

            @Override
            public Triple<A, B, C> toTriple() {
                return new Triple<>(a, null, null);
            }

            @Override
            public Triple<Optional<A>, Optional<B>, Optional<C>> toTripleOfOptionals() {
                return new Triple<>(Optional.of(a), Optional.empty(), Optional.empty());
            }

            @Override
            public A getAOrThrow() {
                return a;
            }

            @Override
            public B getBOrThrow() {
                throw new NoSuchElementException();
            }

            @Override
            public C getCOrThrow() {
                throw new NoSuchElementException();
            }
        };
    }

    /**
     * Creates an instance of this union that contains a B (non-null).
     */
    public static <A, B, C> Either3<A, B, C> b(B b) throws IllegalArgumentException {
        if (b == null) {
            throw new IllegalArgumentException("argument must not be null");
        }

        return new Either3<A, B, C>() {

            @Override
            public <R> R map(Function<? super A, ? extends R> aFn, Function<? super B, ? extends R> bFn, Function<? super C, ? extends R> cFn) {
                return bFn.apply(b);
            }

            @Override
            @SuppressWarnings("unchecked")
            public <R> Either3<R, B, C> mapA(Function<? super A, ? extends R> aFn) {
                return (Either3<R, B, C>) this;
            }

            @Override
            public <R> Either3<A, R, C> mapB(Function<? super B, ? extends R> bFn) {
                return Either3.b(bFn.apply(b));
            }

            @Override
            @SuppressWarnings("unchecked")
            public <R> Either3<A, B, R> mapC(Function<? super C, ? extends R> cFn) {
                return (Either3<A, B, R>) this;
            }

            @Override
            @SuppressWarnings("unchecked")
            public <R> Either3<R, B, C> andThenE(Function<? super A, ? extends R> aFn) {
                return (Either3<R, B, C>) this;
            }

            @Override
            @SuppressWarnings("unchecked")
            public <R extends Either3<?, B, C>> R andThen(Function<? super A, ? extends R> aFn) {
                return (R) this;
            }

            @Override
            public Either3<A, B, C> andThenDo(Consumer<? super A> aC) {
                return this;
            }

            @Override
            public <R> Either<R, Failure> andThenR(Function<? super A, ? extends PortsFuture<R>> aFn) {
                return Either.b(Failure.of(new IllegalStateException("this Either3 does not store the result of a request")));
            }

            @Override
            public <R> Either3<A, B, R> orElse(Function<? super C, R> cFn) {
                return Either3.b(b);
            }

            @Override
            public Either3<A, B, C> orElseDo(Consumer<? super C> cC) {
                return this;
            }

            @Override
            public Either3<A, B, C> orElseDoOnce(Consumer<? super C> cC) {
                return this;
            }

            @Override
            public Either3<A, B, C> on(Consumer<? super A> aC, Consumer<? super B> bC, Consumer<? super C> cC) {
                bC.accept(b);
                return this;
            }

            @Override
            public Either3<A, B, C> onA(Consumer<? super A> aC) {
                return this;
            }

            @Override
            public Either3<A, B, C> onB(Consumer<? super B> bC) {
                bC.accept(b);
                return this;
            }

            @Override
            public Either3<A, B, C> onC(Consumer<? super C> cC) {
                return this;
            }

            @Override
            public Triple<A, B, C> toTriple() {
                return new Triple<>(null, b, null);
            }

            @Override
            public Triple<Optional<A>, Optional<B>, Optional<C>> toTripleOfOptionals() {
                return new Triple<>(Optional.empty(), Optional.of(b), Optional.empty());
            }

            @Override
            public A getAOrThrow() {
                throw new NoSuchElementException();
            }

            @Override
            public B getBOrThrow() {
                return b;
            }

            @Override
            public C getCOrThrow() {
                throw new NoSuchElementException();
            }
        };
    }

    /**
     * Creates an instance of this union that contains a C (non-null).
     */
    public static <A, B, C> Either3<A, B, C> c(C c) throws IllegalArgumentException {
        if (c == null) {
            throw new IllegalArgumentException("argument must not be null");
        }

        return new Either3<A, B, C>() {

            @Override
            public <R> R map(Function<? super A, ? extends R> aFn, Function<? super B, ? extends R> bFn, Function<? super C, ? extends R> cFn) {
                return cFn.apply(c);
            }

            @Override
            @SuppressWarnings("unchecked")
            public <R> Either3<R, B, C> mapA(Function<? super A, ? extends R> aFn) {
                return (Either3<R, B, C>) this;
            }

            @Override
            @SuppressWarnings("unchecked")
            public <R> Either3<A, R, C> mapB(Function<? super B, ? extends R> bFn) {
                return (Either3<A, R, C>) this;
            }

            @Override
            public <R> Either3<A, B, R> mapC(Function<? super C, ? extends R> cFn) {
                return Either3.c(cFn.apply(c));
            }

            @Override
            @SuppressWarnings("unchecked")
            public <R> Either3<R, B, C> andThenE(Function<? super A, ? extends R> aFn) {
                return (Either3<R, B, C>) this;
            }

            @Override
            @SuppressWarnings("unchecked")
            public <R extends Either3<?, B, C>> R andThen(Function<? super A, ? extends R> aFn) {
                return (R) this;
            }

            @Override
            public Either3<A, B, C> andThenDo(Consumer<? super A> aC) {
                return this;
            }

            @Override
            public <R> Either<R, Failure> andThenR(Function<? super A, ? extends PortsFuture<R>> aFn) {
                return c instanceof Failure
                        ? Either.b((Failure) c)
                        : Either.b(Failure.of(new IllegalStateException("this Either3 does not store the result of a request")));
            }

            @Override
            public <R> Either3<A, B, R> orElse(Function<? super C, R> cFn) {
                if (c instanceof Failure) {
                    ((Failure) c).setHasAlreadyBeenHandled();
                }

                return Either3.c(cFn.apply(c));
            }

            @Override
            public Either3<A, B, C> orElseDo(Consumer<? super C> cC) {
                if (c instanceof Failure) {
                    ((Failure) c).setHasAlreadyBeenHandled();
                }

                cC.accept(c);
                return this;
            }

            @Override
            public Either3<A, B, C> orElseDoOnce(Consumer<? super C> cC) {
                if (c instanceof Failure) {
                    Failure failure = (Failure) c;

                    if (failure.hasAlreadyBeenHandled()) {
                        return this;
                    }

                    failure.setHasAlreadyBeenHandled();
                }

                cC.accept(c);
                return this;
            }

            @Override
            public Either3<A, B, C> on(Consumer<? super A> aC, Consumer<? super B> bC, Consumer<? super C> cC) {
                cC.accept(c);
                return this;
            }

            @Override
            public Either3<A, B, C> onA(Consumer<? super A> aC) {
                return this;
            }

            @Override
            public Either3<A, B, C> onB(Consumer<? super B> bC) {
                return this;
            }

            @Override
            public Either3<A, B, C> onC(Consumer<? super C> cC) {
                cC.accept(c);
                return this;
            }

            @Override
            public Triple<A, B, C> toTriple() {
                return new Triple<>(null, null, c);
            }

            @Override
            public Triple<Optional<A>, Optional<B>, Optional<C>> toTripleOfOptionals() {
                return new Triple<>(Optional.empty(), Optional.empty(), Optional.of(c));
            }

            @Override
            public A getAOrThrow() {
                throw new NoSuchElementException();
            }

            @Override
            public B getBOrThrow() {
                throw new NoSuchElementException();
            }

            @Override
            public C getCOrThrow() {
                return c;
            }
        };
    }
}
