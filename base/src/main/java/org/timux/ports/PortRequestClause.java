package org.timux.ports;

public class PortRequestClause<I, O> {

    private final ProtocolParserState state;

    PortRequestClause(ProtocolParserState state) {
        this.state = state;
    }

    public ConditionOrAction<?> call(I payload) {
        Protocol.registerOrExecuteTriggerOrCallActionDependingOnParseState(payload, state);
        return new ConditionOrAction<>(state);
    }
}
