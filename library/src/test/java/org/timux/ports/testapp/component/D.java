package org.timux.ports.testapp.component;

import org.timux.ports.In;

public class D {

    @In void testIn(String string) {
        System.out.println("D received " + string);
    }
}
