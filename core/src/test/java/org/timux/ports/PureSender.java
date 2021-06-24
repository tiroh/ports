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
import org.timux.ports.types.Failure;

public class PureSender {

    @Out
    private Request<PureEitherRequest, Either<Integer, Failure>> pureEitherRequest;

    @Out
    private Request<PureStatelessRequest, Integer> pureStatelessRequest;

    public Either<Integer, Failure> runCall(int payload) {
        return pureEitherRequest.call(new PureEitherRequest(payload));
    }

    public Either<Integer, Failure> runCallE(int payload) {
        return pureEitherRequest.callE(new PureEitherRequest(payload)).getAOrThrow();
    }

    public Either<Integer, Failure> runCallF(int payload) {
        return pureEitherRequest.callF(new PureEitherRequest(payload)).get();
    }

    public Integer runStatelessRequest() {
        return pureStatelessRequest.call(new PureStatelessRequest());
    }
}
