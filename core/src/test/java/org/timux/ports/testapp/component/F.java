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

import org.timux.ports.Event;
import org.timux.ports.Ports;
import org.timux.ports.In;
import org.timux.ports.Out;

public class F {

    @Out Event<IntEvent> intEvent;
    @Out Event<StringEvent> stringEvent;

    @Out Event<DataHasBeenSentEvent> dataHasBeenSentEvent;

    class TestA {

        @Out Event<IntEvent> intEvent;

        public void doWork() {
            intEvent.trigger(new IntEvent(100));
        }
    }

    class TestB {

        @In void onInt(IntEvent event) {
            System.out.println("TestB received " + event.getData());
        }
    }

    public void doWork() {
        TestA a = new TestA();
        TestB b = new TestB();

        Ports.connect(a).and(b);

        a.doWork();

        for (int i = 0; i < 3; i++) {
            intEvent.trigger(new IntEvent(i));
            stringEvent.trigger(new StringEvent("data-" + i));
        }

        dataHasBeenSentEvent.trigger(new DataHasBeenSentEvent());
    }
}
