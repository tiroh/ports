package org.timux.ports.protocol.syntax;

import org.timux.ports.protocol.Protocol;

public class PortRequestClause<I, O> {

    private final Protocol protocol;

    PortRequestClause(Protocol protocol) {
        this.protocol = protocol;
    }

    public ConditionOrAction<?> call(I payload) {
        Protocol.registerCall(payload);
        return new ConditionOrAction<>(protocol);
    }
}
