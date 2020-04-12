package org.timux.ports.testapp.component;

import org.timux.ports.Event;
import org.timux.ports.Out;

public class C {

    @Out Event<StringEvent> stringEvent;
    @Out Event<IntEvent> intEvent;

    public void doStringWork() {
        stringEvent.trigger(new StringEvent("org.timux.Test message"));
    }

    public void doIntWork() {
        intEvent.trigger(new IntEvent(370));
    }
}
