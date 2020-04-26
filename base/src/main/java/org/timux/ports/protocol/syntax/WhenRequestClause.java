package org.timux.ports.protocol.syntax;

import org.timux.ports.protocol.Protocol;

import java.util.function.Predicate;

public class WhenRequestClause<I, O> {

    public ActionRequestClause<I, O> requests(Predicate<I> predicate) {
        Protocol.registerConditionOnSent(predicate);
        return new ActionRequestClause<>();
    }

    public ActionRequestClause<I, O> requests() {
        Protocol.registerConditionOnSent(x -> true);
        return new ActionRequestClause<>();
    }

    public ActionClause<O> responds(Predicate<O> predicate) {
        Protocol.registerConditionOnReceived(predicate);
        return new ActionClause<>();
    }

    public ActionClause<O> responds() {
        Protocol.registerConditionOnReceived(x -> true);
        return new ActionClause<>();
    }
}
