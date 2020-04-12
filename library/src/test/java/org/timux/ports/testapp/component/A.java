package org.timux.ports.testapp.component;

import org.timux.ports.Event;
import org.timux.ports.In;
import org.timux.ports.Out;
import org.timux.ports.Request;

public class A {

    class TestRequest {

    }

    @Out Event<ObjectEvent> objectEvent;
    @Out Event<IntEvent> intEvent;
    @Out Request<ShortRequest, Double> shortRequest;
    @Out Request<ObjectRequest, Object> objectRequest;
    @Out Request<TestCommand, Boolean> testCommand;

    private int field = 47;

    public A() {
        this(3);
        System.out.println("A con");
    }

    public A(int i) {
        System.out.println("A con " + i);

        if (intEvent != null) {
            System.out.println("nicht null");
        }

        System.out.println("Ende A");
    }

    @In void onInt(IntEvent event) {
        field *= 2;
        System.out.println("A received test input: " + event.getData() + ", private field is " + field);
    }

    public void doWork() {
        intEvent.trigger(new IntEvent(37));
        objectEvent.trigger(new ObjectEvent(3700));
        double d = shortRequest.call(new ShortRequest((short) 2));
        Object o = objectRequest.call(new ObjectRequest(9));
        Object o2 = objectRequest.call(new ObjectRequest(null));
        System.out.println("A got replies: " + d + " and " + o + ", " + o2);
    }
}