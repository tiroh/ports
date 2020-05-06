package org.timux.ports;

class PortRequestClause<I, O> {

    public ConditionOrAction<?> call(I payload) {
        Protocol.registerOrExecuteTriggerOrCallActionDependingOnParseState(payload);
        return new ConditionOrAction<>();
    }
}
