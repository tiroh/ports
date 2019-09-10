package org.timux.ports.testapp.component;

import org.timux.ports.Async;
import org.timux.ports.Event;
import org.timux.ports.In;
import org.timux.ports.Out;
import org.timux.ports.SyncLevel;

@Async(multiplicity = 2, syncLevel = SyncLevel.PORT)
public class J {

    @Out
    private Event<Void> doneEvent;

    @In
    private void onIntEvent(Integer data) {
        System.out.println(String.format("Counting to %d in instance %s, thread %d.", data, this, Thread.currentThread().getId()));

        try {
            for (int i = 1; i <= data; i++) {
                System.out.println(String.format("  i = %d, instance %s, Thread %d", i, this, Thread.currentThread().getId()));
                Thread.sleep(2000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        doneEvent.trigger(null);
    }

    @In
    private void onDoubleEvent(Double data) {
        System.out.println(String.format("Received double %f in instance %s, thread %d.", data, this, Thread.currentThread().getId()));
    }

    @In
    private Integer onIntRequest(Integer data) {
        System.out.println(String.format("Received request with %d in instance %s, thread %d.", data, this, Thread.currentThread().getId()));
        return data + 1;
    }
}
