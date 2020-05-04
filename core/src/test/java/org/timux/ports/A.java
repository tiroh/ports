package org.timux.ports;

import org.timux.ports.testapp.component.IntEvent;

class A {

    @Out
    public Event<IntEvent> intEvent;

    @In
    @AsyncPort
    private Double onDoubleRequest(DoubleRequest request) {
        System.out.println("A doublerequest");
        return 1.5 * request.getData();
    }
}
