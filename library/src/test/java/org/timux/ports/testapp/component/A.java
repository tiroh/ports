package org.timux.ports.testapp.component;

import org.timux.ports.*;

public class A {

    @Out Event test;
    @Out Event<Integer> output;
    @Out Request<Short, Double> request;
    @Out Request testRequest;

    private int field = 47;

    public A() {
        this(3);
        System.out.println("A con");
    }

    public A(int i) {
        System.out.println("A con " + i);

        if (output != null) {
            System.out.println("nicht null");
        }

        System.out.println("Ende A");
    }

    @In void onTestInput(Integer payload) {
        field *= 2;
        System.out.println("A received test input: " + payload + ", private field is " + field);
    }

    public void doWork() {
        output.trigger(37);
        test.trigger(3700);
        double d = request.call((short) 2);
        Object o = testRequest.call(9);
        System.out.println("A got replies: " + d + " and " + o);
    }
}