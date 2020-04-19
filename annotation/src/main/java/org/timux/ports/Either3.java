package org.timux.ports;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class Either3<A, B, C> {

    private Either3() {
        //
    }

    public abstract <R> R map(Function<? super A, ? extends R> aFn, Function<? super B, ? extends R> bFn, Function<? super C, ? extends R> cFn);
    public abstract void do_(Consumer<? super A> aC, Consumer<? super B> bC, Consumer<? super C> cC);

    public Optional<A> getA() {
        return map(Optional::ofNullable, b -> Optional.empty(), c -> Optional.empty());
    }

    public Optional<B> getB() {
        return map(a -> Optional.empty(), Optional::ofNullable, c -> Optional.empty());
    }

    public Optional<C> getC() {
        return map(a -> Optional.empty(), b -> Optional.empty(), Optional::ofNullable);
    }

    @Override
    public String toString() {
        return map(String::valueOf, String::valueOf, String::valueOf);
    }

    public static <A, B, C> Either3<A, B, C> a(A a) {
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

    public static <A, B, C> Either3<A, B, C> b(B b) {
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

    public static <A, B, C> Either3<A, B, C> c(C c) {
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
