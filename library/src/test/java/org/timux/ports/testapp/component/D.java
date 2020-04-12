package org.timux.ports.testapp.component;

import org.timux.ports.In;

public class D {

    @In void onString(StringEvent event) {
        System.out.println("D received " + event.getString());
    }
}
