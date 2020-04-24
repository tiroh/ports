package org.timux.ports.protocol.syntax;

import org.timux.ports.protocol.Protocol;

import java.util.function.Predicate;

public class WhenRequestClause<I, O> {

    private final Protocol protocol;

    public WhenRequestClause(Protocol protocol) {
        this.protocol = protocol;
    }

    public ActionClause sends(Predicate<I> predicate) {
        Protocol.registerConditionOnSent(predicate);
        return new ActionClause<>(protocol);
    }

    public ActionClause receives(Predicate<O> predicate) {
        Protocol.registerConditionOnReceived(predicate);
        return new ActionClause<>(protocol);
    }
}
