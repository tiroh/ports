package org.timux.ports.types;

import java.util.Objects;
import java.util.function.Function;

/**
 * Represents a function that accepts three arguments and produces a result.
 * This is the three-arity specialization of {@link java.util.function.BiFunction}.
 *
 * @since 0.5.5
 */
@FunctionalInterface
public interface TriFunction<S, T, U, R> {

    R apply(S s, T t, U u);

    default <V> TriFunction<S, T, U, V> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (S s, T t, U u) -> after.apply(apply(s, t, u));
    }
}
