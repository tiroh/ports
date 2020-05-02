package org.timux.ports.protocol;

import org.timux.ports.Protocol;

public class PortRequestClause<I, O> {

    public ConditionOrAction<?> call(I payload) {
        Protocol.registerOrExecuteTriggerOrCallActionDependingOnParseState(payload);
        return new ConditionOrAction<>();
    }
}
