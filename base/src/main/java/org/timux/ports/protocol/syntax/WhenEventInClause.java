package org.timux.ports.protocol.syntax;

import org.timux.ports.protocol.Protocol;

import java.util.function.Predicate;

public class WhenEventInClause<T> {

    private final Protocol protocol;

    public WhenEventInClause(Protocol protocol) {
        this.protocol = protocol;
    }

    public ActionClause<T> receives(Predicate<T> predicate) {
        return new ActionClause<>(protocol);
    }
}
