package org.timux.ports;

class PortEventClause<T> {

    public ConditionOrAction<?> trigger(T payload) {
        Protocol.registerOrExecuteTriggerOrCallActionDependingOnParseState(payload);
        return new ConditionOrAction<>();
    }
}
