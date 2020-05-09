package org.timux.ports;

class PortEventClause<T> {

    private final ProtocolParserState state;

    PortEventClause(ProtocolParserState state) {
        this.state = state;
    }

    public ConditionOrAction<?> trigger(T payload) {
        Protocol.registerOrExecuteTriggerOrCallActionDependingOnParseState(payload, state);
        return new ConditionOrAction<>(state);
    }
}
