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

import org.timux.ports.types.Either;
import org.timux.ports.types.Either3;

import java.util.Objects;
import java.util.function.Consumer;

public class ConditionOrAction<T> {

    private final ProtocolParserState state;

    ConditionOrAction(ProtocolParserState state) {
        this.state = state;
    }

    public <U> WhenOutClause<U> when(Class<U> eventType) {
        Objects.requireNonNull(eventType);
        TypeUtils.verifyResponseType(eventType, void.class);
        state.registerMessageType(eventType.getName());
        return new WhenOutClause<>(state);
    }

    public <I, O> WhenRequestClause<I, O> when(Class<I> requestType, Class<O> responseType) {
        Objects.requireNonNull(requestType);
        Objects.requireNonNull(responseType);

        TypeUtils.verifyResponseType(requestType, responseType);
        state.registerMessageType(requestType.getName());
        return new WhenRequestClause<>(state);
    }

    public <I, O1, O2> WhenRequestClause<I, Either<O1, O2>> when(Class<I> requestType, Class<O1> responseTypeA, Class<O2> responseTypeB) {
        Objects.requireNonNull(requestType);
        Objects.requireNonNull(responseTypeA);
        Objects.requireNonNull(responseTypeB);

        TypeUtils.verifyResponseType(requestType, Either.class, responseTypeA, responseTypeB);
        state.registerMessageType(requestType.getName());
        return new WhenRequestClause<>(state);
    }

    public <I, O1, O2, O3> WhenRequestClause<I, Either3<O1, O2, O3>> when(
            Class<I> requestType, Class<O1> responseTypeA, Class<O2> responseTypeB, Class<O3> responseTypeC)
    {
        Objects.requireNonNull(requestType);
        Objects.requireNonNull(responseTypeA);
        Objects.requireNonNull(responseTypeB);
        Objects.requireNonNull(responseTypeC);

        TypeUtils.verifyResponseType(requestType, Either3.class, responseTypeA, responseTypeB, responseTypeC);
        state.registerMessageType(requestType.getName());
        return new WhenRequestClause<>(state);
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

    public <U> PortEventClause<U> with(Class<U> eventType, Object owner) {
        Objects.requireNonNull(eventType);
        TypeUtils.verifyResponseType(eventType, void.class);
        state.registerWithMessageTypeAndOwner(eventType.getName(), void.class.getName(), owner);
        return new PortEventClause<>(state);
    }

    public <U> PortEventClause<U> with(Class<U> messageType) {
        return with(messageType, (Object) null);
    }

    <I, O> PortRequestClause<I, O> with(Class<I> requestType, Class<O> responseType, Object owner) {
        Objects.requireNonNull(requestType);
        Objects.requireNonNull(responseType);

        TypeUtils.verifyResponseType(requestType, responseType);
        state.registerWithMessageTypeAndOwner(requestType.getName(), responseType.getName(), owner);
        return new PortRequestClause<>(state);
    }

    public <I, O1, O2> PortRequestClause<I, Either<O1, O2>> with(Class<I> requestType, Class<O1> responseTypeA, Class<O2> responseTypeB) {
        Objects.requireNonNull(requestType);
        Objects.requireNonNull(responseTypeA);
        Objects.requireNonNull(responseTypeB);

        TypeUtils.verifyResponseType(requestType, Either.class, responseTypeA, responseTypeB);
        state.registerWithMessageTypeAndOwner(requestType.getName(), responseTypeA.getName(), responseTypeB.getName(), null);
        return new PortRequestClause<>(state);
    }

    <I, O1, O2> PortRequestClause<I, Either<O1, O2>> with(Class<I> requestType, Class<O1> responseTypeA, Class<O2> responseTypeB, Object owner) {
        Objects.requireNonNull(requestType);
        Objects.requireNonNull(responseTypeA);
        Objects.requireNonNull(responseTypeB);

        TypeUtils.verifyResponseType(requestType, Either.class, responseTypeA, responseTypeB);
        state.registerWithMessageTypeAndOwner(requestType.getName(), responseTypeA.getName(), responseTypeB.getName(), owner);
        return new PortRequestClause<>(state);
    }

    public <I, O1, O2, O3> PortRequestClause<I, Either3<O1, O2, O3>> with(
            Class<I> requestType, Class<O1> responseTypeA, Class<O2> responseTypeB, Class<O3> responseTypeC)
    {
        Objects.requireNonNull(requestType);
        Objects.requireNonNull(responseTypeA);
        Objects.requireNonNull(responseTypeB);
        Objects.requireNonNull(responseTypeC);

        TypeUtils.verifyResponseType(requestType, Either3.class, responseTypeA, responseTypeB, responseTypeC);
        state.registerWithMessageTypeAndOwner(requestType.getName(), responseTypeA.getName(), responseTypeB.getName(), responseTypeC.getName(), null);
        return new PortRequestClause<>(state);
    }

    public <I, O> PortRequestClause<I, O> with(Class<I> requestType, Class<O> responseType) {
        Objects.requireNonNull(requestType);
        Objects.requireNonNull(responseType);

        TypeUtils.verifyResponseType(requestType, responseType);
        state.registerWithMessageTypeAndOwner(requestType.getName(), responseType.getName(), null);
        return new PortRequestClause<>(state);
    }
}
