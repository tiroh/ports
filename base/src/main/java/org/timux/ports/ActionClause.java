package org.timux.ports;

import org.timux.ports.types.Container;

import java.util.function.Consumer;

public class ActionClause<T> {

    private final ProtocolParserState state;

    ActionClause(ProtocolParserState state) {
        this.state = state;
    }

    public ConditionOrAction<T> do_(Action<T> action) {
        state.registerAction(action);
        return new ConditionOrAction<>(state);
    }

    public ConditionOrAction<T> do_(Consumer<T> action) {
        state.registerAction((Action<T>) (payload, owner) -> action.accept(payload));
        return new ConditionOrAction<>(state);
    }

    public ConditionOrAction<T> do_(Runnable action) {
        state.registerAction((Action<T>) (payload, owner) -> action.run());
        return new ConditionOrAction<>(state);
    }

    public ConditionOrAction<T> storeIn(Container<T> container) {
        state.registerAction((Action<T>) (payload, owner) -> container.value = payload);
        return new ConditionOrAction<>(state);
    }

    <U> PortEventClause<U> with(Class<U> messageType, Object owner) {
        state.registerWithMessageTypeAndOwner(messageType.getName(), void.class.getName(), owner);
        return new PortEventClause<>(state);
    }

    public <U> PortEventClause<U> with(Class<U> messageType) {
        return with(messageType, (Object) null);
    }

    <I, O> PortRequestClause<I, O> with(Class<I> requestType, Class<O> responseType, Object owner) {
        state.registerWithMessageTypeAndOwner(requestType.getName(), responseType.getName(), owner);
        return new PortRequestClause<>(state);
    }

    public <I, O> PortRequestClause<I, O> with(Class<I> requestType, Class<O> responseType) {
        return with(requestType, responseType, null);
    }
}
