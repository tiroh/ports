package org.timux.ports;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.timux.ports.testapp.DoubleRequest;
import org.timux.ports.testapp.component.IntEvent;

public class PortsTest {

    class A {

        @Out Event<IntEvent> intEvent;

        @In Double onDoubleRequest(DoubleRequest request) {
            return 1.5 * request.getData();
        }
    }

    class B {

        double receivedData = 0;

        @Out Request<DoubleRequest, Double> doubleRequest;

        @In void onInt(IntEvent event) {
            receivedData = doubleRequest.call(new DoubleRequest(event.getData()));
        }
    }

    class C {

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
}
