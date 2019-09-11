package org.timux.ports;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Tim Rohlfs
 * @since 0.4.0
 */
class Threading {

    private static Map<Object, ThreadQueue> queues = new HashMap<>(); // maps a receiver to its event port queue

    static <T> Consumer<T> enqueueEvent(Object portOwner, Consumer<T> port, int multiplicity, int syncLevel) {
        ThreadQueue threadQueue = getQueue(portOwner, multiplicity, syncLevel);
        return x -> threadQueue.enqueue(x, port);
    }

    private static <T> ThreadQueue getQueue(Object portOwner, int multiplicity, int syncLevel) {
        ThreadQueue threadQueue = queues.get(portOwner);

        if (threadQueue == null) {
            threadQueue = new ThreadQueue(portOwner, multiplicity, syncLevel);
            queues.put(portOwner, threadQueue);
        }

        return threadQueue;
    }

    static void shutdown() {
        queues.values().forEach(ThreadQueue::shutdown);
    }

    static void kill() {
        queues.values().forEach(ThreadQueue::kill);
    }
}
