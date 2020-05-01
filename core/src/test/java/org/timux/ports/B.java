package org.timux.ports;

import org.timux.ports.testapp.component.IntEvent;

class B {

    public double receivedData = 0;

    @Out
    public Request<DoubleRequest, Double> doubleRequest;

    @In
    private void onInt(IntEvent event) {
        receivedData = doubleRequest.call(new DoubleRequest(event.getData()));
    }
}
