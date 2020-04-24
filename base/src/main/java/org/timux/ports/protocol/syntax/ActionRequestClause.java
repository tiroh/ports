package org.timux.ports.protocol.syntax;

import org.timux.ports.protocol.Action;
import org.timux.ports.protocol.Protocol;

import java.util.function.Function;

public class ActionRequestClause<I, O> {

    private final Protocol protocol;

    public ActionRequestClause(Protocol protocol) {
        this.protocol = protocol;
    }

    public void do_(Action action) {
        action.execute();
    }

    public ExpectRequestClause<I, O> expect(Function<I, O> inPort) {
        return new ExpectRequestClause<>(protocol);
    }

    public void return_(O value) {

    }

    public Condition and() {
        return new Condition(protocol);
    }
}
