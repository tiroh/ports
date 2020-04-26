package org.timux.ports.protocol.syntax;

import org.timux.ports.Event;
import org.timux.ports.Request;
import org.timux.ports.protocol.Protocol;

import java.util.function.Consumer;

public class ActionClause<T> {

    public ConditionOrAction<T> do_(Action<T> action) {
        Protocol.registerAction(action);
        return new ConditionOrAction<>();
    }

    public ConditionOrAction<T> do_(Consumer<T> action) {
        Protocol.registerAction((Action<T>) (payload, owner) -> action.accept(payload));
        return new ConditionOrAction<>();
    }

    public ConditionOrAction<T> do_(Runnable action) {
        Protocol.registerAction((Action<T>) (payload, owner) -> action.run());
        return new ConditionOrAction<>();
    }

    public <U> PortEventClause<U> with(Class<U> messageType) {
        Protocol.registerWithMessageType(messageType.getName());
        return new PortEventClause<>();
    }

    public <I, O> PortRequestClause<I, O> with(Class<I> requestType, Class<O> responseType) {
        Protocol.registerWithMessageType(requestType.getName());
        return new PortRequestClause<>();
    }
}
