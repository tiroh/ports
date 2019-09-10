package org.timux.ports.testapp.component;

import org.timux.ports.Event;
import org.timux.ports.In;
import org.timux.ports.Out;
import org.timux.ports.Request;

public class H {

    @Out
    private Event<Integer> intEvent;

    @Out
    private Event<Double> doubleEvent;

    @Out
    private Request<Integer, Integer> intRequest;

    private int eventCounter = 0;

    @In
    private void onDoneEvent(Void nothing) {
        System.out.println("Received done event.");
        eventCounter++;
    }

    public void doWork() {
        for (int i = 0; i < 3; i++) {
            intEvent.trigger(3);
            doubleEvent.trigger(2.5);
        }

        int response = intRequest.call(3);
        System.out.println(String.format("Received response %d.", response));
    }

    public void waitForEvents() {
        while (eventCounter < 3) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                //
            }
        }
    }
}
