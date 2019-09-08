package org.timux.ports.testapp;

import org.timux.ports.Event;
import org.timux.ports.Out;
import org.timux.ports.Request;

public class H {

    @Out
    private Event<Integer> intEvent;

    @Out
    private Request<Integer, Integer> intRequest;

    public void doWork() {
        for (int i = 0; i < 3; i++) {
            intEvent.trigger(3);
        }

        int response = intRequest.call(3);
        System.out.println(String.format("Received response %d.", response));
    }
}
