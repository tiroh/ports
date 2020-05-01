package org.timux.ports;

import org.timux.ports.testapp.component.IntEvent;

class A {

    @Out
    public Event<IntEvent> intEvent;

    @In
    private Double onDoubleRequest(DoubleRequest request) {
        return 1.5 * request.getData();
    }
}
