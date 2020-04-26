package org.timux.ports.protocol.syntax;

import org.timux.ports.Event;
import org.timux.ports.Request;
import org.timux.ports.protocol.Protocol;

import java.util.function.Function;

public class ActionRequestClause<I, O> {

    public ConditionOrAction<?> do_(Action<I> action) {
        Protocol.registerAction(action);
        return new ConditionOrAction<>();
    }

    public ConditionOrAction<I> respond(Function<I, O> response) {
        Protocol.registerRespondAction(response);
        return new ConditionOrAction<>();
    }

    public ConditionOrAction<I> respond(O response) {
        Protocol.registerRespondAction(x -> response);
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
