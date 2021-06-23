/*
 * Copyright 2018-2021 Tim Rohlfs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
