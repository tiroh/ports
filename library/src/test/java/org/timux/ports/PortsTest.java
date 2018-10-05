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

    @Test
    public void smokeTest() {
        A a = new A();
        B b = new B();

        Ports.connect(a).and(b);

        a.out1.trigger(3);

        Assert.assertThat(b.receivedData, IsEqual.equalTo(4.5));
    }
}
