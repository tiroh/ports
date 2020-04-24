package org.timux.ports.protocol.syntax;

import org.timux.ports.protocol.Protocol;

public class ReceivesEventClause {

    private final Protocol protocol;

    public ReceivesEventClause(Protocol protocol) {
        this.protocol = protocol;
    }

    public void within(long milliseconds) {

    }
}
