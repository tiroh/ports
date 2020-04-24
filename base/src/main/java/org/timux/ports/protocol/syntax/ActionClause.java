package org.timux.ports.protocol.syntax;

import org.timux.ports.Event;
import org.timux.ports.Request;
import org.timux.ports.protocol.Action;
import org.timux.ports.protocol.ExpectEventClause;
import org.timux.ports.protocol.Protocol;

import java.util.function.Consumer;

public class ActionClause<T> {

    private final Protocol protocol;

    public ActionClause(Protocol protocol) {
        this.protocol = protocol;
    }

    public <U> ExpectEventClause<U> expect(Consumer<U> inPort) {
        return new ExpectEventClause<>(protocol);
    }

    public ConditionOrAction<T> do_(Action<T> action) {
        Protocol.registerAction(action);
        return new ConditionOrAction<>(protocol);
    }

    public <T> PortEventClause<T> with(Event<T> port) {
        Protocol.registerWithPort(port);
        return new PortEventClause<>(protocol);
    }

    public <I, O> PortRequestClause<I, O> with(Request<I, O> port) {
        Protocol.registerWithPort(port);
        return new PortRequestClause<>(protocol);
    }
}
