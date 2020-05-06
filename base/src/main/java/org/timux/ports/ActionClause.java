package org.timux.ports;

import java.util.function.Consumer;

class ActionClause<T> {

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
