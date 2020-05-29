package org.timux.ports;

import java.util.function.Predicate;

public class WhenOutClause<T> {

    private final ProtocolParserState state;

    WhenOutClause(ProtocolParserState state) {
        this.state = state;
    }

    public ActionClause<T> triggers(Predicate<T> predicate) {
        Protocol.registerConditionOnSent(predicate, state);
        return new ActionClause<>(state);
    }

    public ActionClause<T> triggers() {
        Protocol.registerConditionOnSent(x -> true, state);
        return new ActionClause<>(state);
    }
}
