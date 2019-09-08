package org.timux.ports;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

class ThreadQueue {

    private final Consumer port;
    private final Deque queue = new ArrayDeque();
    private final Deque<PortsThread> threadStack = new ArrayDeque<>();
    private boolean isShutdown = false;

    private class PortsThread extends Thread {

        @Override
        public void run() {
            while (!isShutdown) {
                Object payload;

                synchronized (queue) {
                    while (queue.isEmpty()) {
                        try {
                            queue.wait();
                        } catch (InterruptedException e) {
                            if (isShutdown) {
                                return;
                            }
                        }
                    }

                    if (isShutdown) {
                        return;
                    }

                    payload = queue.remove();
                }

                port.accept(payload);
            }
        }
    }

    ThreadQueue(Consumer port, int multiplicity) {
        this.port = port;

        for (int i = 0; i < multiplicity; i++) {
            threadStack.push(new PortsThread());
            threadStack.peek().start();
        }
    }

    void enqueue(Object data) {
        synchronized (queue) {
            queue.add(data);
            queue.notify();
        }
    }

    void shutdown() {
        synchronized (queue) {
            isShutdown = true;
            queue.notifyAll();
        }
    }

    void kill() {
        isShutdown = true;
        threadStack.forEach(Thread::interrupt);
    }
}
