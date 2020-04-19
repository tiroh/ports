package org.timux.ports;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class Either<A, B> {

    private Either() {
        //
    }

    public abstract <R> R map(Function<? super A, ? extends R> aFn, Function<? super B, ? extends R> bFn);
    public abstract void do_(Consumer<? super A> aC, Consumer<? super B> bC);

    public Optional<A> getA() {
        return map(Optional::ofNullable, b -> Optional.empty());
    }

    public Optional<B> getB() {
        return map(a -> Optional.empty(), Optional::ofNullable);
    }

    @Override
    public String toString() {
        return map(String::valueOf, String::valueOf);
    }

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