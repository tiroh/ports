package org.timux.ports.protocol.syntax;

import org.timux.ports.protocol.Protocol;

public class ReceivesRequestClause<I, O> {

    private final Protocol protocol;

    public ReceivesRequestClause(Protocol protocol) {
        this.protocol = protocol;
    }

    public ActionRequestClause within(long milliseconds) {
        return new ActionRequestClause(protocol);
    }

    public void return_(O value) {

    }
}
