package org.timux.ports;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

/**
 * @author Tim Rohlfs
 * @since 0.4.0
 */
class ThreadQueue {

    private final Object portOwner;
    private final int syncLevel;
    private final Deque<DataQueueEntry> dataQueue = new ArrayDeque<>();
    private final Deque<PortsThread> threadStack = new ArrayDeque<>();
    private boolean isShutdown = false;

    private static class DataQueueEntry {

        Consumer port;
        Object payload;

        public DataQueueEntry(Object payload, Consumer port) {
            this.port = port;
            this.payload = payload;
        }
    }

    private class PortsThread extends Thread {

        @Override
        public void run() {
            while (!isShutdown) {
                DataQueueEntry dataQueueEntry;

                synchronized (dataQueue) {
                    while (dataQueue.isEmpty()) {
                        try {
                            dataQueue.wait();
                        } catch (InterruptedException e) {
                            ;
                        }

                        if (isShutdown) {
                            return;
                        }
                    }

                    dataQueueEntry = dataQueue.poll();
                }

                switch (syncLevel) {
                    case SyncLevel.NONE:
                        dataQueueEntry.port.accept(dataQueueEntry.payload);
                        break;

                    case SyncLevel.COMPONENT:
                        synchronized (portOwner) {
                            dataQueueEntry.port.accept(dataQueueEntry.payload);
                        }

                        break;

                    case SyncLevel.PORT:
                        synchronized (dataQueueEntry.port) {
                            dataQueueEntry.port.accept(dataQueueEntry.payload);
                        }

                        break;

                    default:
                        throw new IllegalStateException("unhandled sync level: " + syncLevel);
                }
            }
        }
    }

    ThreadQueue(Object portOwner, int multiplicity, int syncLevel) {
        this.portOwner = portOwner;
        this.syncLevel = syncLevel;

        for (int i = 0; i < multiplicity; i++) {
            threadStack.push(new PortsThread());
            threadStack.peek().start();
        }
    }

    void enqueue(Object data, Consumer port) {
        synchronized (dataQueue) {
            dataQueue.add(new DataQueueEntry(data, port));
            dataQueue.notify();
        }
    }

    void shutdown() {
        synchronized (dataQueue) {
            isShutdown = true;
            dataQueue.notifyAll();
        }
    }

    void kill() {
        isShutdown = true;
        threadStack.forEach(Thread::interrupt);
    }
}
