package org.timux.ports.protocol.syntax;

import org.timux.ports.protocol.Protocol;

public class PortRequestClause<I, O> {

    public ConditionOrAction<?> call(I payload) {
        Protocol.registerCallAction(payload);
        return new ConditionOrAction<>();
    }
}
