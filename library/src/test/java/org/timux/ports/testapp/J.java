package org.timux.ports.testapp;

import org.timux.ports.Async;
import org.timux.ports.In;

@Async(multiplicity = 2)
public class J {

    @In
    private void onIntEvent(Integer data) {
        System.out.println(String.format("Counting to %d in thread %d.", data, Thread.currentThread().getId()));

        try {
            for (int i = 1; i <= data; i++) {
                System.out.println(String.format("  i = %d, Thread %d", i, Thread.currentThread().getId()));
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @In
    private Integer onIntRequest(Integer data) {
        System.out.println(String.format("Received request with %d in thread %d.", data, Thread.currentThread().getId()));
        return data + 1;
    }
}
