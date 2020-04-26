package org.timux.ports.protocol.syntax;

import org.timux.ports.Protocol;

public class PortEventClause<T> {

    public ConditionOrAction<?> trigger(T payload) {
        Protocol.registerOrExecuteTriggerOrCallActionDependingOnParseState(payload);
        return new ConditionOrAction<>();
    }
}
