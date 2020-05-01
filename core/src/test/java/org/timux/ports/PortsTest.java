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

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.timux.ports.testapp.component.IntEvent;

public class PortsTest {

    static class A {

        @Out Event<IntEvent> intEvent;

        @In Double onDoubleRequest(DoubleRequest request) {
            return 1.5 * request.getData();
        }
    }

    static class B {

        double receivedData = 0;

        @Out Request<DoubleRequest, Double> doubleRequest;

        @In void onInt(IntEvent event) {
            receivedData = doubleRequest.call(new DoubleRequest(event.getData()));
        }
    }

    static class C {

        int data;

        @In void onInt(IntEvent event) {
            this.data = event.getData();
        }
    }

    static class ValueContainer<T> {

        T value;

        ValueContainer(T defaultValue) {
            value = defaultValue;
        }
    }

    @Before
    public void setup() {
        Ports.releaseProtocols();
    }

    @Test
    public void smokeTest() {
        A a = new A();
        B b = new B();

        Ports.connect(a).and(b);

        a.intEvent.trigger(new IntEvent(3));

        Assert.assertThat(b.receivedData, IsEqual.equalTo(4.5));
    }

    @Test
    public void multipleReceiversWithoutReconnection() {
        A a = new A();
        C c1 = new C();
        C c2 = new C();

        Ports.connect(a).and(c1);
        Ports.connect(a).and(c2);

        a.intEvent.trigger(new IntEvent(3));

        Assert.assertThat(c1.data, IsEqual.equalTo(3));
        Assert.assertThat(c2.data, IsEqual.equalTo(0));
    }

    @Test
    public void multipleReceiversWithReconnection() {
        A a = new A();
        C c1 = new C();
        C c2 = new C();

        Ports.connect(a).and(c1);
        Ports.connect(a).and(c2, PortsOptions.FORCE_CONNECT_EVENT_PORTS);

        a.intEvent.trigger(new IntEvent(3));

        Assert.assertThat(c1.data, IsEqual.equalTo(3));
        Assert.assertThat(c2.data, IsEqual.equalTo(c1.data));
    }

    @Test
    public void disconnect() {
        A a = new A();
        C c1 = new C();
        C c2 = new C();

        Ports.connect(a).and(c1);
        Ports.connect(a).and(c2, PortsOptions.FORCE_CONNECT_EVENT_PORTS);

        Ports.disconnect(a).and(c1);

        a.intEvent.trigger(new IntEvent(3));

        Assert.assertThat(c1.data, IsEqual.equalTo(0));
        Assert.assertThat(c2.data, IsEqual.equalTo(3));
    }

    @Test
    public void protocols() {
        A a = new A();
        B b = new B();

        Ports.connect(a).and(b);

        Ports.register(a, b);

        ValueContainer<Boolean> firstActionA = new ValueContainer<>(false);
        ValueContainer<Boolean> secondActionA = new ValueContainer<>(false);
        ValueContainer<Boolean> firstActionB = new ValueContainer<>(false);
        ValueContainer<Boolean> secondActionB = new ValueContainer<>(false);
        ValueContainer<Boolean> firstActionC = new ValueContainer<>(false);
        ValueContainer<Boolean> firstActionD = new ValueContainer<>(false);

        ValueContainer<Integer> doubleRequestIndex = new ValueContainer<>(0);
        double[] doubleRequestValues = {50.0, 2.1, 2.0, 2.1, 4.0};

        ValueContainer<Boolean> doubleRequestResponse = new ValueContainer<>(false);

        Ports.protocol()
            .when(IntEvent.class).triggers(x -> x.getData() > 1)
                .do_((x, owner) ->
                        firstActionA.value = (!firstActionA.value && x.getData() == 4 && owner == a)
                                || (firstActionA.value && x.getData() == 2 && owner != a))
                .do_((x, owner) ->
                        secondActionA.value = (!secondActionA.value && x.getData() == 4 && owner == a) ||
                                (secondActionA.value && x.getData() == 2 && owner != a))
            .when(IntEvent.class).triggers(x -> x.getData() > 2)
                .do_((x, owner) -> firstActionB.value = !firstActionB.value && x.getData() == 4 && owner == a)
                .do_((x, owner) -> secondActionB.value = !secondActionB.value && x.getData() == 4 && owner == a);

        Ports.protocol()
            .when(IntEvent.class).triggers(x -> x.getData() > 3)
                .do_((x, owner) -> firstActionC.value = !firstActionC.value && x.getData() == 4 && owner == a)
                .with(DoubleRequest.class, Double.class).call(new DoubleRequest(50.0))
                .with(IntEvent.class).trigger(new IntEvent(2))
            .when(DoubleRequest.class).triggers(x -> x.getData() >= 4.0)
                .do_((x, owner) ->
                        firstActionD.value = (!firstActionD.value && x.getData() == 50.0 && owner != b)
                                || (firstActionD.value && x.getData() == 4.0 && owner == b))
            .when(DoubleRequest.class, Double.class).requests(x -> x.getData() > 3.0)
                .respond(17.5)
            .when(DoubleRequest.class, Double.class).responds(x -> x > 5.0)
                .do_((x, owner) ->
                        doubleRequestResponse.value = (!doubleRequestResponse.value && x == 17.5 && owner != b)
                                || (doubleRequestResponse.value && x == 17.5 && owner == b));

        Ports.protocol()
            .when(IntEvent.class).triggers(x -> x.getData() > 1)
                .with(DoubleRequest.class, Double.class, b).call(new DoubleRequest(2.1))
            .when(DoubleRequest.class, Double.class).requests()
                .do_(x -> {
                    if (x.getData() == doubleRequestValues[doubleRequestIndex.value]) {
                        doubleRequestIndex.value++;
                    }
                });

        Ports.protocol()
            .with(IntEvent.class, a).trigger(new IntEvent(4));

        Assert.assertTrue(firstActionA.value);
        Assert.assertTrue(secondActionA.value);
        Assert.assertTrue(firstActionB.value);
        Assert.assertTrue(secondActionB.value);
        Assert.assertTrue(firstActionC.value);
        Assert.assertTrue(firstActionD.value);
        Assert.assertEquals(5, doubleRequestIndex.value.intValue());
        Assert.assertTrue(doubleRequestResponse.value);
    }
}
