package org.timux.ports.protocol.syntax;

import java.util.function.Predicate;

public class WhenEventInClause<T> {

    public ActionClause<T> receives(Predicate<T> predicate) {
        return new ActionClause<>();
    }
}
