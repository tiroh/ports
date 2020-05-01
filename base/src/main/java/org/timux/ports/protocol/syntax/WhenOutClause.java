package org.timux.ports.protocol.syntax;

import org.timux.ports.Protocol;

import java.util.function.Predicate;

public class WhenOutClause<T> {

    public ActionClause<T> triggers(Predicate<T> predicate) {
        Protocol.registerConditionOnSent(predicate);
        return new ActionClause<>();
    }

    public ActionClause<T> triggers() {
        Protocol.registerConditionOnSent(x -> true);
        return new ActionClause<>();
    }
}
