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
import org.junit.Test;
import org.timux.ports.testapp.DoubleRequest;
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
    public void protocol() {
        A a = new A();
        B b = new B();

        Ports.connect(a).and(b);

        Ports.protocol(a, b);

        Ports.protocol()
            .when(IntEvent.class).sends(x -> x.getData() > 1)
                .do_((x, owner) -> System.out.println(String.format("a.intEvent sends x > 1 (%d), first action (%s)", x.getData(), owner.getClass().getName())))
                .do_((x, owner) -> System.out.println(String.format("a.intEvent sends x > 1 (%d), second action (%s)", x.getData(), owner.getClass().getName())))
            .when(IntEvent.class).sends(x -> x.getData() > 2)
                .do_((x, owner) -> System.out.println(String.format("a.intEvent sends x > 2 (%d), first action (%s)", x.getData(), owner.getClass().getName())))
                .do_((x, owner) -> System.out.println(String.format("a.intEvent sends x > 2 (%d), second action (%s)", x.getData(), owner.getClass().getName())));

        Ports.protocol()
            .when(IntEvent.class).sends(x -> x.getData() > 3)
                .do_((x, owner) -> System.out.println(String.format("a.intEvent sends x > 3 (%d) (%s)", x.getData(), owner.getClass().getName())))
                .with(DoubleRequest.class, Double.class).call(new DoubleRequest(5.0))
                .with(IntEvent.class).trigger(new IntEvent(2))
            .when(DoubleRequest.class).sends(x -> x.getData() >= 4.0)
                .do_((x, owner) -> System.out.println(String.format("b.doubleRequest request: x >= 4.0 (%f) (%s)", x.getData(), owner.getClass().getName())))
            .when(DoubleRequest.class, Double.class).requests(x -> x.getData() > 3.0)
                .respond(17.5)
            .when(DoubleRequest.class, Double.class).responds(x -> x > 5.0)
                .do_((x, owner) -> System.out.println(String.format("b.doubleRequest receives x > 5.0 (%f) (%s)", x, owner.getClass().getName())));

//        Ports.protocol()
//            .when(IntEvent.class).sends(x -> x.getData() > 1)
//                .with(b.doubleRequest).call(new DoubleRequest(2.1));

        a.intEvent.trigger(new IntEvent(4));
    }
}
