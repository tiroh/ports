package org.timux.ports.testapp.component;

import org.timux.ports.Event;
import org.timux.ports.Ports;
import org.timux.ports.In;
import org.timux.ports.Out;

public class F {

    @Out Event<Integer> outInt;
    @Out Event<String> outStr;

    @Out Event<Void> dataHasBeenSent;

    class TestA {

        @Out Event<Integer> testPort;

        public void doWork() {
            testPort.trigger(100);
        }
    }

    class TestB {

        @In void testPort(Integer n) {
            System.out.println("TestB received " + n);
        }
    }

    public void doWork() {
        TestA a = new TestA();
        TestB b = new TestB();

        Ports.connect(a).and(b);

        a.doWork();

        for (int i = 0; i < 3; i++) {
            outInt.trigger(i);
            outStr.trigger("data-" + i);
        }

        dataHasBeenSent.trigger(null);
    }
}
