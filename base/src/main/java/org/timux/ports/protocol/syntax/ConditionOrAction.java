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

import org.timux.ports.Event;
import org.timux.ports.Request;
import org.timux.ports.protocol.Action;
import org.timux.ports.protocol.ExpectEventClause;
import org.timux.ports.protocol.Protocol;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class ConditionOrAction {

    private final Protocol protocol;

    public ConditionOrAction(Protocol protocol) {
        this.protocol = protocol;
    }

    public <T> WhenEventOutClause<T> when(Event<T> port) {
        Protocol.registerConditionPort(port);
        return new WhenEventOutClause<>(protocol);
    }

    public <I, O> WhenRequestClause<I, O> when(Request<I, O> port) {
        Protocol.registerConditionPort(port);
        return new WhenRequestClause<>(protocol);
    }

    public <T> WhenEventInClause<T> when(Consumer<T> port) {
        return new WhenEventInClause<>(protocol);
    }

    public <I, O> WhenRequestClause<I, O> when(Function<I, O> port) {
        return new WhenRequestClause<>(protocol);
    }

    public <U> ExpectEventClause<U> expect(Consumer<U> inPort) {
        return new ExpectEventClause<>(protocol);
    }

    public <T, I, O> ExpectRequestClause<I, O> expect(Class<T> clazz, BiFunction<T, I, O> inPort) {
        System.err.println(MethodDetector.getMethod(clazz, inPort));
        return new ExpectRequestClause<>(protocol);
    }

    public ConditionOrAction do_(Action action) {
        Protocol.registerAction(action);
        return new ConditionOrAction(protocol);
    }

    public <T> PortEventClause<T> with(Event<T> port) {
        Protocol.registerWithPort(port);
        return new PortEventClause<>(protocol);
    }

    public <I, O> PortRequestClause<I, O> with(Request<I, O> port) {
        Protocol.registerWithPort(port);
        return new PortRequestClause<>(protocol);
    }
}
