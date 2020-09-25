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
import org.timux.ports.Response;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A union type for two types A and B.
 *
 * <p> Use multiple {@link Response} annotations on a request type in order to indicate the
 * use of this union type.
 *
 * @param <A> The first type.
 * @param <B> The second type.
 * @see Either3
 * @see Nothing
 * @see Unknown
 * @see Pair
 * @see Triple
 * @since 0.4.1
 */
public abstract class Either<A, B> {

    private Either() {
        //
    }

    public static <T> Either<T, Nothing> ofNullable(T t) {
        return t == null ? Either.b(Nothing.INSTANCE) : Either.a(t);
    }

    public static <T> Either<T, Nothing> ofOptional(Optional<T> optional) {
        return Either.ofNullable(optional.orElse(null));
    }

    public static <T, U> Either<T, U> ofOptional(Optional<T> optional, U orElse) {
        return optional.isPresent() ? Either.a(optional.get()) : Either.b(orElse);
    }

    public static <T> Either<Success, T> success() {
        return Either.a(Success.INSTANCE);
    }

    public static <T> Either<T, Failure> failure() {
        return Either.b(Failure.INSTANCE);
    }

    public static <T> Either<T, Failure> failure(String message) {
        return Either.b(Failure.of(message));
    }

    public static <T> Either<T, Failure> failure(Throwable throwable) {
        return Either.b(Failure.of(throwable));
    }

    public static <T> Either<T, Failure> failure(String message, Throwable throwable) {
        return Either.b(Failure.of(message, throwable));
    }
    
    public static <T> Either<T, Nothing> nothing() {
        return Either.b(Nothing.INSTANCE);
    }
    
    public static <T> Either<T, Unknown> unknown() {
        return Either.b(Unknown.INSTANCE);
    }

    /**
     * Maps the constituents of this union to R.
     */
    public abstract <R> R map(Function<? super A, ? extends R> aFn, Function<? super B, ? extends R> bFn);

    /**
     * Maps the A constituent, if it exists, to R.
     */
    public abstract <R> Either<R, B> mapA(Function<? super A, ? extends R> aFn);

    /**
     * Maps the B constituent, if it exists, to R.
     */
    public abstract <R> Either<A, R> mapB(Function<? super B, ? extends R> bFn);

    /**
     * Maps the A constituent, if it exists, to R, wrapped into a new {@link Either}.
     *
     * @see #andThen
     * @see #andThenR
     */
    public abstract <R> Either<R, B> andThenE(Function<? super A, ? extends R> aFn);

    /**
     * Maps the A constituent, if it exists, to R, which must be an {@link Either} that has the
     * same B type like this {@link Either}.
     */
    public abstract <R extends Either<?, B>> R andThen(Function<? super A, ? extends R> aFn);

    /**
     * Applies the provided consumer to the A constituent, if it exists, or does nothing otherwise.
     */
    public abstract Either<A, B> andThenDo(Consumer<? super A> aC);

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
    // TODO this is probably not necessary anymore because of the callE and callF methods of the Request class
    public abstract <R> Either<R, Failure> andThenR(Function<? super A, ? extends PortsFuture<R>> aFn);

    /**
     * Maps the B constituent, if it exists, to R.
     */
    public abstract <R> Either<A, R> orElse(Function<? super B, R> bFn);

    /**
     * Applies the provided consumer to the B constituent, if it exists, or does nothing otherwise.
     *
     * @see #orElseDoOnce
     */
    public abstract Either<A, B> orElseDo(Consumer<? super B> bC);

    /**
     * If the B constituent is a {@link Failure}, this method applies the provided
     * consumer to that failure only if it has not already been handled by
     * another call of {@link #orElseDoOnce}, {@link #orElse}, or {@link #orElseDo}.
     * Otherwise, this method behaves exactly like {@link #orElseDo}.
     */
    public abstract Either<A, B> orElseDoOnce(Consumer<? super B> bC);

    /**
     * Executes the provided actions on the constituents of this union.
     */
    public abstract Either<A, B> on(Consumer<? super A> aC, Consumer<? super B> bC);

    /**
     * Executes the provided action on the A constituent, if it exists, and returns this union.
     */
    public abstract Either<A, B> onA(Consumer<? super A> aC);

    /**
     * Executes the provided action on the B constituent, if it exists, and returns this union.
     */
    public abstract Either<A, B> onB(Consumer<? super B> bC);

    /**
     * Returns a {@link Pair} that represents the current state of this union, indicating a missing
     * value with null.
     */
    public abstract Pair<A, B> toPair();

    /**
     * Returns a {@link Pair} that represents the current state of this union, indicating a missing
     * value with an empty {@link Optional}.
     */
    public abstract Pair<Optional<A>, Optional<B>> toPairOfOptionals();

    /**
     * Returns the A constituent of this union in the form of an {@link Optional}.
     */
    public Optional<A> getA() {
        return map(Optional::ofNullable, b -> Optional.empty());
    }

    /**
     * Returns the B constituent of this union in the form of an {@link Optional}.
     */
    public Optional<B> getB() {
        return map(a -> Optional.empty(), Optional::ofNullable);
    }

    /**
     * Returns true if this union represents an instance of {@link Success},
     * and false otherwise.
     */
    public boolean isSuccess() {
        return map(a -> a.getClass() == Success.class, b -> b.getClass() == Success.class);
    }

    /**
     * Returns true if this union represents an instance of {@link Failure},
     * and false otherwise.
     */
    public boolean isFailure() {
        return map(a -> a.getClass() == Failure.class, b -> b.getClass() == Failure.class);
    }

    /**
     * Returns true if this union represents an instance of {@link Nothing},
     * and false otherwise.
     */
    public boolean isNothing() {
        return map(a -> a.getClass() == Nothing.class, b -> b.getClass() == Nothing.class);
    }

    /**
     * Returns true if this union represents an instance of {@link Unknown},
     * and false otherwise.
     */
    public boolean isUnknown() {
        return map(a -> a.getClass() == Unknown.class, b -> b.getClass() == Unknown.class);
    }

    /**
     * Executes the provided runnable and returns this union.
     */
    public Either<A, B> finallyDo(Runnable runnable) {
        runnable.run();
        return this;
    }

    @Override
    public String toString() {
        return map(String::valueOf, String::valueOf);
    }

    /**
     * Creates an instance of this union that contains an A (non-null).
     */
    public static <A, B> Either<A, B> a(A a) {
        if (a == null) {
            throw new IllegalArgumentException("argument must not be null");
        }

        return new Either<A, B>() {

            @Override
            public <R> R map(Function<? super A, ? extends R> aFn, Function<? super B, ? extends R> bFn) {
                return aFn.apply(a);
            }

            @Override
            public <R> Either<R, B> mapA(Function<? super A, ? extends R> aFn) {
                return Either.a(aFn.apply(a));
            }

            @Override
            @SuppressWarnings("unchecked")
            public <R> Either<A, R> mapB(Function<? super B, ? extends R> bFn) {
                return (Either<A, R>) this;
            }

            @Override
            public <R> Either<R, B> andThenE(Function<? super A, ? extends R> aFn) {
                return Either.a(aFn.apply(a));
            }

            @Override
            public <R extends Either<?, B>> R andThen(Function<? super A, ? extends R> aFn) {
                return aFn.apply(a);
            }

            @Override
            public Either<A, B> andThenDo(Consumer<? super A> aC) {
                aC.accept(a);
                return this;
            }

            @Override
            public <R> Either<R, Failure> andThenR(Function<? super A, ? extends PortsFuture<R>> aFn) {
                return aFn.apply(a).getE();
            }

            @Override
            public <R> Either<A, R> orElse(Function<? super B, R> bFn) {
                return Either.a(a);
            }

            @Override
            public Either<A, B> orElseDo(Consumer<? super B> bC) {
                return this;
            }

            @Override
            public Either<A, B> orElseDoOnce(Consumer<? super B> bC) {
                return this;
            }

            @Override
            public Either<A, B> on(Consumer<? super A> aC, Consumer<? super B> bC) {
                aC.accept(a);
                return this;
            }

            @Override
            public Either<A, B> onA(Consumer<? super A> aC) {
                aC.accept(a);
                return this;
            }

            @Override
            public Either<A, B> onB(Consumer<? super B> bC) {
                return this;
            }

            @Override
            public Pair<A, B> toPair() {
                return new Pair<>(a, null);
            }

            @Override
            public Pair<Optional<A>, Optional<B>> toPairOfOptionals() {
                return new Pair<>(Optional.of(a), Optional.empty());
            }
        };
    }

    /**
     * Creates an instance of this union that contains a B (non-null).
     */
    public static <A, B> Either<A, B> b(B b) {
        if (b == null) {
            throw new IllegalArgumentException("argument must not be null");
        }

        return new Either<A, B>() {

            @Override
            public <R> R map(Function<? super A, ? extends R> aFn, Function<? super B, ? extends R> bFn) {
                return bFn.apply(b);
            }

            @Override
            @SuppressWarnings("unchecked")
            public <R> Either<R, B> mapA(Function<? super A, ? extends R> aFn) {
                return (Either<R, B>) this;
            }

            @Override
            public <R> Either<A, R> mapB(Function<? super B, ? extends R> bFn) {
                return Either.b(bFn.apply(b));
            }

            @Override
            @SuppressWarnings("unchecked")
            public <R> Either<R, B> andThenE(Function<? super A, ? extends R> aFn) {
                return (Either<R, B>) this;
            }

            @Override
            @SuppressWarnings("unchecked")
            public <R extends Either<?, B>> R andThen(Function<? super A, ? extends R> aFn) {
                return (R) this;
            }

            @Override
            public Either<A, B> andThenDo(Consumer<? super A> aC) {
                return this;
            }

            @Override
            public <R> Either<R, Failure> andThenR(Function<? super A, ? extends PortsFuture<R>> aFn) {
                return b instanceof Failure
                        ? Either.b((Failure) b)
                        : Either.b(Failure.of(new IllegalStateException("this Either does not store the result of a request")));
            }

            @Override
            public <R> Either<A, R> orElse(Function<? super B, R> bFn) {
                if (b instanceof Failure) {
                    ((Failure) b).setHasAlreadyBeenHandled();
                }

                return Either.b(bFn.apply(b));
            }

            @Override
            public Either<A, B> orElseDo(Consumer<? super B> bC) {
                if (b instanceof Failure) {
                    ((Failure) b).setHasAlreadyBeenHandled();
                }

                bC.accept(b);
                return this;
            }

            @Override
            public Either<A, B> orElseDoOnce(Consumer<? super B> bC) {
                if (b instanceof Failure) {
                    Failure failure = (Failure) b;

                    if (failure.hasAlreadyBeenHandled()) {
                        return this;
                    }

                    failure.setHasAlreadyBeenHandled();
                }

                bC.accept(b);
                return this;
            }

            @Override
            public Either<A, B> on(Consumer<? super A> aC, Consumer<? super B> bC) {
                bC.accept(b);
                return this;
            }

            @Override
            public Either<A, B> onA(Consumer<? super A> aC) {
                return this;
            }

            @Override
            public Either<A, B> onB(Consumer<? super B> bC) {
                bC.accept(b);
                return this;
            }

            @Override
            public Pair<A, B> toPair() {
                return new Pair<>(null, b);
            }

            @Override
            public Pair<Optional<A>, Optional<B>> toPairOfOptionals() {
                return new Pair<>(Optional.empty(), Optional.of(b));
            }
       };
    }
}