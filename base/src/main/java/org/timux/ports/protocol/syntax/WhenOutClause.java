package org.timux.ports.protocol.syntax;

import org.timux.ports.protocol.Protocol;

import java.util.function.Predicate;

public class WhenOutClause<T> {

    public ActionClause<T> sends(Predicate<T> predicate) {
        Protocol.registerConditionOnSent(predicate);
        return new ActionClause<>();
    }

    public ActionClause<T> sends() {
        Protocol.registerConditionOnSent(x -> true);
        return new ActionClause<>();
    }
}
