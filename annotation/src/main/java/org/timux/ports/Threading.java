package org.timux.ports;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

class Threading {

    private static Map<Object, ThreadQueue> queues = new HashMap<>();

    static <T> Consumer<T> execute(Object component, Consumer<T> port, int multiplicity, int syncLevel) {
        ThreadQueue threadQueue = queues.get(component);

        if (threadQueue == null) {
            threadQueue = new ThreadQueue(component, multiplicity, syncLevel);
            queues.put(component, threadQueue);
        }

        final ThreadQueue finalThreadQueue = queues.get(component);

        return x -> finalThreadQueue.enqueue(x, port);
    }

    static void shutdown() {
        queues.values().forEach(ThreadQueue::shutdown);
    }

    static void kill() {
        queues.values().forEach(ThreadQueue::kill);
    }
}
