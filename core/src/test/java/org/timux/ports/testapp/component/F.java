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
