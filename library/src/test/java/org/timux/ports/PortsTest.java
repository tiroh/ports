package org.timux.ports;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;


public class PortsTest {

    class A {

        @Out Event<Integer> out1;

        @In Double in2(Double data) {
            return 1.5 * data;
        }
    }

    class B {

        double receivedData = 0;

        @Out Request<Double, Double> out2;

        @In void in1(Integer data) {
            receivedData = out2.call(data.doubleValue());
        }
    }

    class C {

        int data;

        @In void in1(Integer data) {
            this.data = data;
        }
    }

    class D {

        static final int NUMBER = 20000000;

        int data = 0;
        int tmp = 0;

        @Out Event<Integer> intEvent;

        @In void onResponseEvent(Integer data) {
            // Do some complicated stuff in order to provoke race conditions.
            tmp = data;
            this.data += 5;
            tmp += 5;
            this.data += data;
            this.data -= tmp - data;
        }

        void execute() {
            for (int i = 0; i < NUMBER; i++) {
                intEvent.trigger(-1);
            }
        }
    }

    @Async(multiplicity = 100, syncLevel = SyncLevel.NONE)
    class E {

        @Out Event<Integer> responseEvent;

        @In void onIntEvent(Integer data) {
            responseEvent.trigger(data + 2);
        }
    }

    @Test
    public void smokeTest() {
        A a = new A();
        B b = new B();

        Ports.connect(a).and(b);

        a.out1.trigger(3);

        Assert.assertThat(b.receivedData, IsEqual.equalTo(4.5));
    }

    @Test
    public void multipleReceiversWithoutReconnection() {
        A a = new A();
        C c1 = new C();
        C c2 = new C();

        Ports.connect(a).and(c1);
        Ports.connect(a).and(c2);

        a.out1.trigger(3);

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

        a.out1.trigger(3);

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

        a.out1.trigger(3);

        Assert.assertThat(c1.data, IsEqual.equalTo(0));
        Assert.assertThat(c2.data, IsEqual.equalTo(3));
    }

    @Test
    public void asynchronousSenderShouldNotCauseRaceConditionsInSynchronousReceiver() {
        D d = new D();
        E e = new E();

        Ports.connect(d).and(e);

        new Thread(d::execute).start();

        try {
            Thread.sleep(60000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        Assert.assertThat(d.data, IsEqual.equalTo(D.NUMBER));
    }
}
