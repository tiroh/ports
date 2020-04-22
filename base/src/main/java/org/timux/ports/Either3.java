package org.timux.ports;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A union type for three types A, B, and C. The primary use case is as a response type for a
 * {@link Request} that may return different kinds of data, depending on the situation.
 * <p>
 * Use multiple {@link Response} annotations on a request type in order to indicate the
 * use of this union type.
 *
 * @see Either
 * @see SuccessOrFailure
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

    /**
     * Maps the constituents of this union to R.
     */
    public abstract <R> R map(Function<? super A, ? extends R> aFn, Function<? super B, ? extends R> bFn, Function<? super C, ? extends R> cFn);

    /**
     * Executes the provided actions on the constituents of this union.
     */
    public abstract void do_(Consumer<? super A> aC, Consumer<? super B> bC, Consumer<? super C> cC);

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

    @Override
    public String toString() {
        return map(String::valueOf, String::valueOf, String::valueOf);
    }

    /**
     * Creates an instance of this union that contains an A (non-null).
     */
    public static <A, B, C> Either3<A, B, C> a(A a) {
        if (a == null) {
            throw new IllegalArgumentException("argument must not be null");
        }

        return new Either3<A, B, C>() {

            @Override
            public <R> R map(Function<? super A, ? extends R> aFn, Function<? super B, ? extends R> bFn, Function<? super C, ? extends R> cFn) {
                return aFn.apply(a);
            }

            @Override
            public void do_(Consumer<? super A> aC, Consumer<? super B> bC, Consumer<? super C> cC) {
                aC.accept(a);
            }
        };
    }

    /**
     * Creates an instance of this union that contains a B (non-null).
     */
    public static <A, B, C> Either3<A, B, C> b(B b) {
        if (b == null) {
            throw new IllegalArgumentException("argument must not be null");
        }

        return new Either3<A, B, C>() {

            @Override
            public <R> R map(Function<? super A, ? extends R> aFn, Function<? super B, ? extends R> bFn, Function<? super C, ? extends R> cFn) {
                return bFn.apply(b);
            }

            @Override
            public void do_(Consumer<? super A> aC, Consumer<? super B> bC, Consumer<? super C> cC) {
                bC.accept(b);
            }
        };
    }

    /**
     * Creates an instance of this union that contains a C (non-null).
     */
    public static <A, B, C> Either3<A, B, C> c(C c) {
        if (c == null) {
            throw new IllegalArgumentException("argument must not be null");
        }

        return new Either3<A, B, C>() {

            @Override
            public <R> R map(Function<? super A, ? extends R> aFn, Function<? super B, ? extends R> bFn, Function<? super C, ? extends R> cFn) {
                return cFn.apply(c);
            }

            @Override
            public void do_(Consumer<? super A> aC, Consumer<? super B> bC, Consumer<? super C> cC) {
                cC.accept(c);
            }
        };
    }
}
