package org.timux.ports.testapp.component;

import org.timux.ports.In;
import org.timux.ports.Queue;
import org.timux.ports.Stack;

public class G {

    @In Queue<Integer> inInt;
    @In Stack<String> inStr;

    @In void onDataHasBeenSent(Void nothing) {
        while (!inInt.isEmpty()) {
            System.out.println("G has in queue: " + inInt.poll());
        }

        while (!inStr.isEmpty()) {
            System.out.println("G has in stack: " + inStr.pop());
        }
    }
}
