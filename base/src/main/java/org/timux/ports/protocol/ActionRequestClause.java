package org.timux.ports.protocol;

import org.timux.ports.Protocol;

import java.util.function.Consumer;
import java.util.function.Function;

public class ActionRequestClause<I, O> {

    public ConditionOrAction<?> do_(Action<I> action) {
        Protocol.registerAction(action);
        return new ConditionOrAction<>();
    }

    public ConditionOrAction<I> do_(Consumer<I> action) {
        Protocol.registerAction((Action<I>) (payload, owner) -> action.accept(payload));
        return new ConditionOrAction<>();
    }

    public ConditionOrAction<I> do_(Runnable action) {
        Protocol.registerAction((Action<I>) (payload, owner) -> action.run());
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

    public <U> PortEventClause<U> with(Class<U> messageType, Object owner) {
        Protocol.registerWithMessageTypeAndOwner(messageType.getName(), void.class.getName(), owner);
        return new PortEventClause<>();
    }

    public <U> PortEventClause<U> with(Class<U> messageType) {
        return with(messageType, (Object) null);
    }

    public <I, O> PortRequestClause<I, O> with(Class<I> requestType, Class<O> responseType, Object owner) {
        Protocol.registerWithMessageTypeAndOwner(requestType.getName(), responseType.getName(), owner);
        return new PortRequestClause<>();
    }

    public <I, O> PortRequestClause<I, O> with(Class<I> requestType, Class<O> responseType) {
        return with(requestType, responseType, null);
    }
}
