package org.timux.ports.protocol.syntax;

import org.timux.ports.protocol.Protocol;

public class PortEventClause<T> {

    private final Protocol protocol;

    PortEventClause(Protocol protocol) {
        this.protocol = protocol;
    }

    public ConditionOrAction trigger(T payload) {
//        protocol.send(payload);
        Protocol.registerTrigger(payload);
        return new ConditionOrAction(protocol);
    }
}
