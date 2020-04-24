package org.timux.ports.protocol.syntax;

import org.timux.ports.protocol.Protocol;

public class ReturnsClause<O> {

    private final Protocol protocol;

    public ReturnsClause(Protocol protocol) {
        this.protocol = protocol;
    }

    public ActionRequestClause within(long milliseconds) {
        return new ActionRequestClause(protocol);
    }

    public void return_(O value) {

    }
}
