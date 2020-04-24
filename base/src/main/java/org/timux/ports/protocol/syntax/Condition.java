package org.timux.ports.protocol.syntax;

import org.timux.ports.Event;
import org.timux.ports.Request;
import org.timux.ports.protocol.Protocol;

import java.util.function.Consumer;
import java.util.function.Function;

public class Condition {

    private final Protocol protocol;

    Condition(Protocol protocol) {
        this.protocol = protocol;
    }

    public <T> WhenEventOutClause<T> when(Event<T> port) {
        return new WhenEventOutClause<>(protocol);
    }

    public <I, O> WhenRequestClause<I, O> when(Request<I, O> port) {
        return new WhenRequestClause<>(protocol);
    }

    public <T> WhenEventInClause<T> when(Consumer<T> port) {
        return new WhenEventInClause<>(protocol);
    }

    public <I, O> WhenRequestClause<I, O> when(Function<I, O> port) {
        return new WhenRequestClause<>(protocol);
    }
}
