/*
 * Copyright 2018-2021 Tim Rohlfs
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

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A union type for two constituent types A and B.
 *
 * <p> Use multiple {@link Response} annotations on a request type in order to indicate the
 * use of this union type.
 *
 * @param <A> The first type.
 * @param <B> The second type.
 * @see Either3
 * @see Empty
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

    /**
     * Returns an {@link Either} representing either the first or the second argument,
     * depending on which one is non-null. If both are non-null, the first one is chosen.
     *
     * @throws IllegalArgumentException if both arguments are null.
     * @see #of(List)
     * @see #of(Pair)
     * @see Either3#ofNullables
     */
    public static <T, U> Either<T, U> of(T t, U u) {
        if (t != null) {
            return Either.a(t);
        }

        if (u != null) {
            return Either.b(u);
        }

        throw new IllegalArgumentException("either the first or the second argument must be non-null, but both are null");
    }

    /**
     * Returns an {@link Either} representing either the first or the second element of the supplied list,
     * depending on which one is non-null. If both are non-null, the first one is chosen. If the list has
     * more than two elements, the others are ignored.
     *
     * @throws IllegalArgumentException if both elements are null.
     * @see #of(Object, Object)
     * @see #of(Pair)
     * @see Either3#ofNullables
     */
    public static <T> Either<T, T> of(List<T> elements) {
        return Either.of(elements.get(0), elements.get(1));
    }

    /**
     * Returns an {@link Either} representing either the first or the second constituent of the supplied pair,
     * depending on which one is non-null. If both are non-null, the first one is chosen.
     *
     * @throws IllegalArgumentException if both constituents are null.
     * @see #of(Object, Object)
     * @see #of(List)
     * @see Either3#ofNullables
     */
    public static <T, U> Either<T, U> of(Pair<T, U> pair) {
        return Either.of(pair.getA(), pair.getB());
    }

    /**
     * Returns an {@link Either} representing either the supplied value, if it is non-null,
     * or {@link Nothing}, if it is null.
     *
     * @see Either3#ofNullables
     */
    public static <T> Either<T, Nothing> ofNullable(T t) {
        return t == null ? Either.b(Nothing.INSTANCE) : Either.a(t);
    }

    public static <T> Either<T, Nothing> ofOptional(Optional<T> optional) {
        return Either.ofNullable(optional.orElse(null));
    }

    public static <T, U> Either<T, U> ofOptional(Optional<T> optional, U orElse) {
        return optional.isPresent() ? Either.a(optional.get()) : Either.b(orElse);
    }

    /**
     * Returns an {@link Either} containing either the provided {@code value} if it is non-null
     * and non-blank, or {@link Empty} if it is null or blank.
     *
     * @see Either3#ofString
     */
    public static Either<String, Empty> ofString(String value) {
        if (value != null) {
            for (int i = value.length() - 1; i >= 0; i--) {
                if (!Character.isWhitespace(value.charAt(i))) {
                    return Either.a(value);
                }
            }
        }

        return Either.b(Empty.INSTANCE);
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

    public static <T> Either<T, Failure> failure(Failure failure) {
        return Either.b(failure);
    }

    public static <T, U> Either<T, Failure> failure(Either<U, Failure> either) {
        return either.map(u -> Either.b(Failure.INSTANCE), Either::b);
    }

    public static <T, U, V> Either<T, Failure> failure(Either3<U, V, Failure> either3) {
        return either3.map(u -> Either.b(Failure.INSTANCE), v -> Either.b(Failure.INSTANCE), Either::b);
    }

    public static <T> Either<T, Empty> empty() {
        return Either.b(Empty.INSTANCE);
    }

    public static <T> Either<T, Nothing> nothing() {
        return Either.b(Nothing.INSTANCE);
    }

    public static <T> Either<T, Unknown> unknown() {
        return Either.b(Unknown.INSTANCE);
    }

    /**
     * Returns an {@link Either} containing either the return value of the {@code  supplier} or
     * a {@link Failure} in case the {@code  supplier} returns null, a Failure, or throws an exception.
     *
     * <p>If you want to handle the case that the {@code supplier} returns null separately from the exception case, use
     * {@link Either3#valueOrNothingOrFailure} instead.
     *
     * <p>If you want to ignore the return value of the {@code supplier}, use {@link #successOrFailure(Supplier)}
     * instead.
     */
    public static <T> Either<T, Failure> valueOrFailure(Supplier<T> supplier) {
        try {
            T t = supplier.get();

            if (t == null) {
                return Either.b(Failure.of("supplier has returned null"));
            } else {
                return t instanceof Failure ? Either.b((Failure) t) : Either.a(t);
            }
        } catch (Exception e) {
            return Either.b(Failure.of(e));
        }
    }

    /**
     * Returns an {@link Either} containing either a {@link Success}, in case the supplied {@code either}
     * contains a {@code T}, or a {@link Failure}, in case the {@code either} contains a {@link Failure}.
     */
    public static <T> Either<Success, Failure> successOrFailure(Either<T, Failure> either) {
        return either.map(t -> Either.a(Success.INSTANCE), Either::b);
    }

    /**
     * Returns an {@link Either} containing either a {@link Success}, in case the supplied {@code either3} contains
     * a {@code T} or an {@code U}, or a {@link Failure}, in case the {@code either3} contains a {@link Failure}.
     */
    public static <T, U> Either<Success, Failure> successOrFailure(Either3<T, U, Failure> either3) {
        return either3.map(t -> Either.a(Success.INSTANCE), u -> Either.a(Success.INSTANCE), Either::b);
    }

    /**
     * Returns an {@link Either} containing either a {@link Success}, in case the {@code supplier} terminates
     * normally, or a {@link Failure}, in case the {@code supplier} throws an exception. The return value of the
     * {@code supplier} is ignored.
     *
     * <p>If you don't want to ignore the return value of the {@code supplier}, use {@link #valueOrFailure} or
     * {@link Either3#valueOrNothingOrFailure} instead.
     *
     * @see #successOrFailure(Runnable)
     */
    public static <T> Either<Success, Failure> successOrFailure(Supplier<T> supplier) {
        try {
            supplier.get();
            return Either.a(Success.INSTANCE);
        } catch (Exception e) {
            return Either.b(Failure.of(e));
        }
    }

    /**
     * Returns an {@link Either} containing either a {@link Success}, in case the {@code action} terminates
     * normally, or a {@link Failure}, in case the {@code action} throws an exception.
     */
    public static <T> Either<Success, Failure> successOrFailure(Runnable action) {
        try {
            action.run();
            return Either.a(Success.INSTANCE);
        } catch (Exception e) {
            return Either.b(Failure.of(e));
        }
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
     * @see #andThenMap
     * @see #andThenR
     */
    public abstract <R> Either<R, B> andThenMapE(Function<? super A, ? extends R> aFn);

    /**
     * Maps the A constituent, if it exists, to R, which must be an {@link Either} that has the
     * same B type like this {@link Either}.
     */
    public abstract <R extends Either<?, B>> R andThenMap(Function<? super A, ? extends R> aFn);

    /**
     * Applies the provided consumer to the A constituent, if it exists, or does nothing otherwise.
     */
    public abstract Either<A, B> andThenDo(Consumer<? super A> aC);

    /**
     * A version of {@link #andThenMap} that supports working with requests. With this method (and together with
     * {@link PortsFuture#andThenE}) you can build chains of requests.
     *
     * <p> It maps the A constituent, if it exists, to (a {@link PortsFuture} R, or returns the B constituent
     * otherwise. In this context, the A constituent is the result of a preceding request.
     *
     * @see PortsFuture#andThenE
     * @see #andThenMap
     * @see #andThenMapE
     * @see #orElseMap
     * @see #orElseDo
     * @see #finallyDo
     */
    // TODO this is probably not necessary anymore because of the callE and callF methods of the Request class
    public abstract <R> Either<R, Failure> andThenR(Function<? super A, ? extends PortsFuture<R>> aFn);

    /**
     * Maps the B constituent, if it exists, to R.
     */
    public abstract <R> Either<A, R> orElseMap(Function<? super B, R> bFn);

    /**
     * Applies the provided consumer to the B constituent, if it exists, or does nothing otherwise.
     *
     * @see #orElseDoOnce
     */
    public abstract Either<A, B> orElseDo(Consumer<? super B> bC);

    /**
     * If the B constituent is a {@link Failure}, this method applies the provided
     * consumer to that failure only if it has not already been handled by
     * another call of {@link #orElseDoOnce}, {@link #orElseMap}, or {@link #orElseDo}.
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
     *
     * @see #getB()
     * @see #getAOrThrow()
     * @see #getBOrThrow()
     */
    public Optional<A> getA() {
        return map(Optional::ofNullable, b -> Optional.empty());
    }

    /**
     * Returns the B constituent of this union in the form of an {@link Optional}.
     *
     * @see #getA()
     * @see #getAOrThrow()
     * @see #getBOrThrow()
     */
    public Optional<B> getB() {
        return map(a -> Optional.empty(), Optional::ofNullable);
    }

    /**
     * Returns the A constituent of this union if it exists. If it doesn't exist, a
     * {@link NoSuchConstituentException} is thrown. If the B constituent of this union
     * represents a {@link Failure} that is equipped with a {@link Throwable}, that Throwable is
     * provided as the cause of the {@link NoSuchConstituentException}.
     *
     * @see #getBOrThrow()
     * @see #getA()
     * @see #getB()
     */
    public abstract A getAOrThrow() throws NoSuchConstituentException;

    /**
     * Returns the B constituent of this union if it exists. If it doesn't exist, a
     * {@link NoSuchConstituentException} is thrown. If the A constituent of this union
     * represents a {@link Failure} that is equipped with a {@link Throwable}, that Throwable is
     * provided as the cause of the {@link NoSuchConstituentException}.
     *
     * @see #getAOrThrow()
     * @see #getA()
     * @see #getB()
     */
    public abstract B getBOrThrow() throws NoSuchConstituentException;

    /**
     * Returns true if this union represents an instance of {@link Success},
     * and false otherwise.
     */
    public boolean isSuccess() {
        return map(
                x -> checkRecursively(x, Success.class, Either::isSuccess, Either3::isSuccess),
                x -> checkRecursively(x, Success.class, Either::isSuccess, Either3::isSuccess));
    }

    /**
     * Returns true if this union represents an instance of {@link Failure},
     * and false otherwise.
     */
    public boolean isFailure() {
        return map(
                x -> checkRecursively(x, Failure.class, Either::isFailure, Either3::isFailure),
                x -> checkRecursively(x, Failure.class, Either::isFailure, Either3::isFailure));
    }

    /**
     * Returns true if this union represents an instance of {@link Empty},
     * and false otherwise.
     */
    public boolean isEmpty() {
        return map(
                x -> checkRecursively(x, Empty.class, Either::isEmpty, Either3::isEmpty),
                x -> checkRecursively(x, Empty.class, Either::isEmpty, Either3::isEmpty));
    }

    /**
     * Returns true if this union represents an instance of {@link Nothing},
     * and false otherwise.
     */
    public boolean isNothing() {
        return map(
                x -> checkRecursively(x, Nothing.class, Either::isNothing, Either3::isNothing),
                x -> checkRecursively(x, Nothing.class, Either::isNothing, Either3::isNothing));
    }

    /**
     * Returns true if this union represents an instance of {@link Unknown},
     * and false otherwise.
     */
    public boolean isUnknown() {
        return map(
                x -> checkRecursively(x, Unknown.class, Either::isUnknown, Either3::isUnknown),
                x -> checkRecursively(x, Unknown.class, Either::isUnknown, Either3::isUnknown));
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
    public static <A, B> Either<A, B> a(A a) throws IllegalArgumentException {
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
            public <R> Either<R, B> andThenMapE(Function<? super A, ? extends R> aFn) {
                return Either.a(aFn.apply(a));
            }

            @Override
            public <R extends Either<?, B>> R andThenMap(Function<? super A, ? extends R> aFn) {
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
            public <R> Either<A, R> orElseMap(Function<? super B, R> bFn) {
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

            @Override
            public A getAOrThrow() {
                return a;
            }

            @Override
            public B getBOrThrow() {
                throwGetOrThrowException(a);
                return null; // unreachable
            }

            @Override
            public boolean equals(Object obj) {
                return Either.equals(this, a, obj);
            }

            @Override
            public int hashCode() {
                return a.hashCode();
            }
        };
    }

    /**
     * Creates an instance of this union that contains a B (non-null).
     */
    public static <A, B> Either<A, B> b(B b) throws IllegalArgumentException {
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
            public <R> Either<R, B> andThenMapE(Function<? super A, ? extends R> aFn) {
                return (Either<R, B>) this;
            }

            @Override
            @SuppressWarnings("unchecked")
            public <R extends Either<?, B>> R andThenMap(Function<? super A, ? extends R> aFn) {
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
            public <R> Either<A, R> orElseMap(Function<? super B, R> bFn) {
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

            @Override
            public A getAOrThrow() {
                throwGetOrThrowException(b);
                return null; // unreachable
            }

            @Override
            public B getBOrThrow() {
                return b;
            }

            @Override
            public boolean equals(Object obj) {
                return Either.equals(this, b, obj);
            }

            @Override
            public int hashCode() {
                return b.hashCode();
            }
        };
    }

    // This method is used by both Either and Either3.
    static void throwGetOrThrowException(Object x) {
        if (x instanceof Failure) {
            Failure failure = (Failure) x;

            Throwable cause = failure.getThrowable().orElse(null);
            String message = failure.getMessage();

            if (cause != null) {
                if (!message.isEmpty()) {
                    throw new NoSuchConstituentException(message, cause);
                }

                throw new NoSuchConstituentException(cause);
            } else if (!message.isEmpty()) {
                throw new NoSuchConstituentException(message);
            }
        }

        throw new NoSuchConstituentException();
    }

    // This method is used by both Either and Either3.
    static boolean checkRecursively(Object x, Class<?> clazz, Predicate<Either<?, ?>> ep, Predicate<Either3<?, ?, ?>> e3p) {
        return x instanceof Either
                ? ep.test((Either<?, ?>) x)
                : (x instanceof Either3 ? e3p.test(((Either3<?, ?, ?>) x)) : x.getClass() == clazz);
    }

    // This method is used by both Either and Either3.
    static boolean equals(Object self, Object value, Object other) {
        if (self == other) {
            return true;
        }

        if (other instanceof Either) {
            return ((Either<?, ?>) other).map(x -> x.equals(value), x -> x.equals(value));
        } else if (other instanceof Either3) {
            return ((Either3<?, ?, ?>) other).map(x -> x.equals(value), x -> x.equals(value), x -> x.equals(value));
        }

        return false;
    }
}