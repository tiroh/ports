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

package org.timux.ports.protocol.syntax;

import org.timux.ports.protocol.Protocol;

import java.util.function.Consumer;

public class ConditionOrAction<T> {

    public <U> WhenOutClause<U> when(Class<U> messageType) {
        Protocol.registerMessageType(messageType.getName());
        return new WhenOutClause<>();
    }

    public <I, O> WhenRequestClause<I, O> when(Class<I> requestType, Class<O> responseType) {
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

    public <U> PortEventClause<U> with(Class<U> messageType) {
        Protocol.registerWithMessageType(messageType.getName());
        return new PortEventClause<>();
    }

    public <I, O> PortRequestClause<I, O> with(Class<I> requestType, Class<O> responseType) {
        Protocol.registerWithMessageType(requestType.getName());
        return new PortRequestClause<>();
    }
}
