package org.timux.ports.protocol.syntax;

import org.timux.ports.protocol.Protocol;

public class PortEventClause<T> {

    public ConditionOrAction<?> trigger(T payload) {
        Protocol.registerTriggerAction(payload);
        return new ConditionOrAction<>();
    }
}
