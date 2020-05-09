package org.timux.ports;

import java.util.function.Predicate;

class WhenRequestClause<I, O> {

    private final ProtocolParserState state;

    WhenRequestClause(ProtocolParserState state) {
        this.state = state;
    }

    public ActionRequestClause<I, O> requests(Predicate<I> predicate) {
        Protocol.registerConditionOnSent(predicate, state);
        return new ActionRequestClause<>(state);
    }

    public ActionRequestClause<I, O> requests() {
        Protocol.registerConditionOnSent(x -> true, state);
        return new ActionRequestClause<>(state);
    }

    public ActionClause<O> responds(Predicate<O> predicate) {
        Protocol.registerConditionOnReceived(predicate, state);
        return new ActionClause<>(state);
    }

    public ActionClause<O> responds() {
        Protocol.registerConditionOnReceived(x -> true, state);
        return new ActionClause<>(state);
    }
}
