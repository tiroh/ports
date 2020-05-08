package org.timux.ports;

import org.timux.ports.testapp.component.IntEvent;

class A {

    public double receivedData = 0;

    @Out
    public Event<IntEvent> intEvent;

    @In
    @AsyncPort
    private Double onDoubleRequest(DoubleRequest request) {
        receivedData = request.getData();
        return 1.5 * request.getData();
    }
}
