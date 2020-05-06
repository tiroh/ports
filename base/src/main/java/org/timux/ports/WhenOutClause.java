package org.timux.ports;

import java.util.function.Predicate;

class WhenOutClause<T> {

    public ActionClause<T> triggers(Predicate<T> predicate) {
        Protocol.registerConditionOnSent(predicate);
        return new ActionClause<>();
    }

    public ActionClause<T> triggers() {
        Protocol.registerConditionOnSent(x -> true);
        return new ActionClause<>();
    }
}
