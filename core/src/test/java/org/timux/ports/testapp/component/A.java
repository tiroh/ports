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

public class A {

    @Out Event<ObjectEvent> objectEvent;
    @Out Event<IntEvent> intEvent;
    @Out Request<ShortRequest, Double> shortRequest;
    @Out Request<ObjectRequest, Object> objectRequest;
    @Out Request<TestCommand, Either<Boolean, Integer>> testCommand;

    private int field = 47;

    public A() {
        this(3);
        System.out.println("A con");
    }

    public A(int i) {
        System.out.println("A con " + i);

        if (intEvent != null) {
            System.out.println("nicht null");
        }

        System.out.println("Ende A");
    }

    @In
    private void onInt(IntEvent event) {
        field *= 2;
        System.out.println("A received test input: " + event.getData() + ", private field is " + field);
    }

    @In
    private void onRuntimeException(RuntimeException exception) {
        System.out.println("Received exception: " + exception.getMessage());
    }

    public void doWork() {
        intEvent.trigger(new IntEvent(37));
        objectEvent.trigger(new ObjectEvent(3700));
        System.out.println(testCommand.call(new TestCommand()).toString());
        double d = shortRequest.call(new ShortRequest((short) 2));
        Object o = objectRequest.call(new ObjectRequest(9));
        Object o2 = objectRequest.call(new ObjectRequest(null));
        System.out.println("A got replies: " + d + " and " + o + ", " + o2);
    }
}