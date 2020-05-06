/*
 * Copyright 2018-2020 Tim Rohlfs
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

import java.util.function.Consumer;

class ConditionOrAction<T> {

    public <U> WhenOutClause<U> when(Class<U> eventType) {
        TypeUtils.verifyResponseType(eventType, void.class);
        Protocol.registerMessageType(eventType.getName());
        return new WhenOutClause<>();
    }

    public <I, O> WhenRequestClause<I, O> when(Class<I> requestType, Class<O> responseType) {
        if (responseType == void.class) {
            throw new IllegalArgumentException("response type must not be void");
        }

        TypeUtils.verifyResponseType(requestType, responseType);
        Protocol.registerMessageType(requestType.getName());
        return new WhenRequestClause<>();
    }

    public <I, O1, O2> WhenRequestClause<I, Either<O1, O2>> when(Class<I> requestType, Class<O1> responseTypeA, Class<O2> responseTypeB) {
        TypeUtils.verifyResponseType(requestType, Either.class, responseTypeA, responseTypeB);
        Protocol.registerMessageType(requestType.getName());
        return new WhenRequestClause<>();
    }

    public <I, O1, O2, O3> WhenRequestClause<I, Either3<O1, O2, O3>> when(
            Class<I> requestType, Class<O1> responseTypeA, Class<O2> responseTypeB, Class<O3> responseTypeC)
    {
        TypeUtils.verifyResponseType(requestType, Either3.class, responseTypeA, responseTypeB, responseTypeC);
        Protocol.registerMessageType(requestType.getName());
        return new WhenRequestClause<>();
    }

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

    public <U> PortEventClause<U> with(Class<U> eventType, Object owner) {
        TypeUtils.verifyResponseType(eventType, void.class);
        Protocol.registerWithMessageTypeAndOwner(eventType.getName(), void.class.getName(), owner);
        return new PortEventClause<>();
    }

    public <U> PortEventClause<U> with(Class<U> messageType) {
        return with(messageType, (Object) null);
    }

    public <I, O> PortRequestClause<I, O> with(Class<I> requestType, Class<O> responseType, Object owner) {
        TypeUtils.verifyResponseType(requestType, responseType);
        Protocol.registerWithMessageTypeAndOwner(requestType.getName(), responseType.getName(), owner);
        return new PortRequestClause<>();
    }

    public <I, O1, O2> PortRequestClause<I, Either<O1, O2>> with(Class<I> requestType, Class<O1> responseTypeA, Class<O2> responseTypeB) {
        TypeUtils.verifyResponseType(requestType, Either.class, responseTypeA, responseTypeB);
        Protocol.registerWithMessageTypeAndOwner(requestType.getName(), Either.class.getName(), null);
        return new PortRequestClause<>();
    }

    public <I, O1, O2> PortRequestClause<I, Either<O1, O2>> with(Class<I> requestType, Class<O1> responseTypeA, Class<O2> responseTypeB, Object owner) {
        TypeUtils.verifyResponseType(requestType, Either.class, responseTypeA, responseTypeB);
        Protocol.registerWithMessageTypeAndOwner(requestType.getName(), Either.class.getName(), owner);
        return new PortRequestClause<>();
    }

    public <I, O1, O2, O3> PortRequestClause<I, Either3<O1, O2, O3>> with(
            Class<I> requestType, Class<O1> responseTypeA, Class<O2> responseTypeB, Class<O3> responseTypeC)
    {
        TypeUtils.verifyResponseType(requestType, Either3.class, responseTypeA, responseTypeB, responseTypeC);
        Protocol.registerWithMessageTypeAndOwner(requestType.getName(), Either3.class.getName(), null);
        return new PortRequestClause<>();
    }

    public <I, O> PortRequestClause<I, O> with(Class<I> requestType, Class<O> responseType) {
        TypeUtils.verifyResponseType(requestType, responseType);
        Protocol.registerWithMessageTypeAndOwner(requestType.getName(), responseType.getName(), null);
        return new PortRequestClause<>();
    }
}
