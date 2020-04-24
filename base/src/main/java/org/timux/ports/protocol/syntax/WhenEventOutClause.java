package org.timux.ports.protocol.syntax;

import org.timux.ports.protocol.Protocol;

import java.util.function.Predicate;

public class WhenEventOutClause<T> {

    private final Protocol protocol;

    public WhenEventOutClause(Protocol protocol) {
        this.protocol = protocol;
    }

    public ActionClause<T> sends(Predicate<T> predicate) {
//        protocol.sends(predicate);
        Protocol.registerConditionOnSent(predicate);
        return new ActionClause<>(protocol);
    }
}
