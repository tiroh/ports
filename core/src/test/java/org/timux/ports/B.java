package org.timux.ports;

import org.timux.ports.testapp.component.IntEvent;

class B {

    public double receivedData = 0;

    @Out
    public Request<DoubleRequest, Double> doubleRequest;

    @In
    @AsyncPort
    private void onInt(IntEvent event) {
        receivedData = doubleRequest.callAsync(new DoubleRequest(event.getData())).get();
    }
}
