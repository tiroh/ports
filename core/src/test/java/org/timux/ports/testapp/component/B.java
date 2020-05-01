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
 
package org.timux.ports.testapp.component;

import org.timux.ports.*;

public class B {

    @Out Event<IntEvent> intEvent;
    @Out Event<RuntimeException> runtimeExceptionEvent;

    public B() {
        System.out.println("B con");
    }

    @In void onInt(IntEvent event) {
        System.out.println("B received input: " + event.getData());
        runtimeExceptionEvent.trigger(new RuntimeException("Test exception with data " + event.getData()));
    }

    @In void onObject(ObjectEvent event) {
        System.out.println("B received input 2: " + event.getObject());
    }

    @In Double onShortRequest(ShortRequest request) {
        System.out.println("B received request: " + request.getData());
        intEvent.trigger(new IntEvent((int) request.getData() + 1));
        return request.getData() * 1.5;
    }

    @In Object onObjectRequest(ObjectRequest request) {
        return "blub(" + request.getObject() + ")";
    }

    @In Either<Boolean, Integer> onTestCommand(TestCommand command) {
        return Either.a(true);
    }

    @In Either<Integer, String> onFragileRequest(FragileRequest request) {
        return SuccessOrFailure.failure("an error message");
    }
}