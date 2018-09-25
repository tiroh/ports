package org.timux.ports.testapp.component;

import org.timux.ports.Event;
import org.timux.ports.In;
import org.timux.ports.Out;

public class B {

    @Out Event<Integer> testPort;

    public B() {
        System.out.println("B con");
    }

    @In void onInput(Integer n) {
        System.out.println("B received input: " + n);
    }

    @In void onInput2(Object n) {
        System.out.println("B received input 2: " + n);
    }

    @In Double onRequest(Short n) {
        System.out.println("B received request: " + n);
        testPort.trigger((int) n + 1);
        return n * 1.5;
    }

    @In Object onTestRequest(Object o) {
        return "blub(" + o + ")";
    }
}