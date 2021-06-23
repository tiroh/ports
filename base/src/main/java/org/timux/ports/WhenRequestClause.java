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

import java.util.function.Predicate;

public class WhenRequestClause<I, O> {

    private final ProtocolParserState state;

    WhenRequestClause(ProtocolParserState state) {
        this.state = state;
    }

    public ActionRequestClause<I, O> requests(Predicate<I> predicate) {
        Protocol.registerConditionOnSent(predicate, state);
        return new ActionRequestClause<>(state);
    }

    public ActionRequestClause<I, O> requests() {
        Protocol.registerConditionOnSent(x -> true, state);
        return new ActionRequestClause<>(state);
    }

    public ActionClause<O> responds(Predicate<O> predicate) {
        Protocol.registerConditionOnReceived(predicate, state);
        return new ActionClause<>(state);
    }

    public ActionClause<O> responds() {
        Protocol.registerConditionOnReceived(x -> true, state);
        return new ActionClause<>(state);
    }
}
