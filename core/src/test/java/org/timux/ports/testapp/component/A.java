package org.timux.ports.testapp.component;

import org.timux.ports.*;

public class A {

    @Out Event<ObjectEvent> objectEvent;
    @Out Event<IntEvent> intEvent;
    @Out Request<ShortRequest, Double> shortRequest;
    @Out Request<ObjectRequest, Object> objectRequest;
    @Out Request<TestCommand, Either<Boolean, Integer>> testCommand;
    @Out Request<FragileRequest, SuccessOrFailure<Integer, String>> fragileRequest;

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

    @In void onRuntimeException(RuntimeException exception) {
        System.out.println("Received exception: " + exception.getMessage());
    }

    public void doWork() {
        intEvent.trigger(new IntEvent(37));
        objectEvent.trigger(new ObjectEvent(3700));
        System.out.println(testCommand.call(new TestCommand()).toString());
        System.out.println(fragileRequest.call(new FragileRequest()).toString());
        double d = shortRequest.call(new ShortRequest((short) 2));
        Object o = objectRequest.call(new ObjectRequest(9));
        Object o2 = objectRequest.call(new ObjectRequest(null));
        System.out.println("A got replies: " + d + " and " + o + ", " + o2);
    }
}