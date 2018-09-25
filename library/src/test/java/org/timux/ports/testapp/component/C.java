package org.timux.ports.testapp.component;

import org.timux.ports.Event;
import org.timux.ports.Out;

public class C {

    @Out Event<String> stringOut;
    @Out Event<Integer> intOut;

    public void doStringWork() {
        stringOut.trigger("org.timux.Test message");
    }

    public void doIntWork() {
        intOut.trigger(370);
    }
}
