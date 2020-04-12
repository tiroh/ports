package org.timux.ports.testapp.component;

import org.timux.ports.In;
import org.timux.ports.Queue;
import org.timux.ports.Stack;

public class G {

    @In Queue<IntEvent> inInt;
    @In Stack<StringEvent> inStr;

    @In void onDataHasBeenSent(DataHasBeenSentEvent event) {
        while (!inInt.isEmpty()) {
            System.out.println("G has in queue: " + inInt.poll().getData());
        }

        while (!inStr.isEmpty()) {
            System.out.println("G has in stack: " + inStr.pop().getString());
        }
    }
}
