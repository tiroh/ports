package org.timux.ports.testapp.component;

import org.timux.ports.In;

public class E {

    @In void testIn(String string) {
        System.out.println("E received String " + string + ", this should only be called in case of reconnect!");
    }

    @In void onMessage(Integer n) {
        System.out.println("E received integer message " + n);
    }
}
