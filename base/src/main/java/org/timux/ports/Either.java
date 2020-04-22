package org.timux.ports;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A union type for two types A and B.
 * <p>
 * Use multiple {@link Response} annotations on a request type in order to indicate the
 * use of this union type.
 * <p>
 * For a {@link Request} that returns either a valid response object or an error status,
 * use the {@link SuccessOrFailure} union type.
 *
 * @see Either3
 * @see SuccessOrFailure
 *
 * @param <A> The first type.
 * @param <B> The second type.
 *
 * @since 0.4.1
 */
public abstract class Either<A, B> {

    protected Either() {
        //
    }

    /**
     * Maps the constituents of this union to R.
     */
    public abstract <R> R map(Function<? super A, ? extends R> aFn, Function<? super B, ? extends R> bFn);

    /**
     * Executes the provided actions on the constituents of this union.
     */
    public abstract void do_(Consumer<? super A> aC, Consumer<? super B> bC);

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

    @Override
    public String toString() {
        return map(String::valueOf, String::valueOf);
    }

    /**
     * Creates an instance of this union that contains an A.
     */
    public static <A, B> Either<A, B> a(A a) {
        return new Either<A, B>() {

            @Override
            public <R> R map(Function<? super A, ? extends R> aFn, Function<? super B, ? extends R> bFn) {
                return aFn.apply(a);
            }

            @Override
            public void do_(Consumer<? super A> aC, Consumer<? super B> bC) {
                aC.accept(a);
            }
        };
    }

    /**
     * Creates an instance of this union that contains a B.
     */
    public static <A, B> Either<A, B> b(B b) {
        return new Either<A, B>() {

            @Override
            public <R> R map(Function<? super A, ? extends R> aFn, Function<? super B, ? extends R> bFn) {
                return bFn.apply(b);
            }

            @Override
            public void do_(Consumer<? super A> aC, Consumer<? super B> bC) {
                bC.accept(b);
            }
        };
    }
}