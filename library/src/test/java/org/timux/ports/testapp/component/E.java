package org.timux.ports.testapp.component;

import org.timux.ports.In;

public class E {

    @In void onString(StringEvent event) {
        System.out.println("E received String " + event.getString() + ", this should only be called in case of reconnect!");
    }

    @In void onInt(IntEvent event) {
        System.out.println("E received integer message " + event.getData());
    }
}
