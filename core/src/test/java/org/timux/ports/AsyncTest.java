package org.timux.ports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

public class AsyncTest {

    private static final int NUMBER_OF_COMPONENTS = 20;

    static class Component {

        private Double doubleState;

        Component() {
            reset();
        }

        @Out
        Request<DoubleRequest, Double> doubleRequest;

        @Out
        Event<DoubleEvent> doubleEvent;

        @In
        private Double onDoubleRequest(DoubleRequest request) {
            System.out.println("request " + this + " receives " + request.getData());

            doubleState *= request.getData() + 0.5;

            if (request.getData() > 0 && doubleRequest != null && doubleEvent != null) {
                System.out.println("request " + this + " submits request " + (request.getData() - 1));
                PortsFuture<Double> future = doubleRequest.submit(new DoubleRequest(request.getData() - 1));
                System.out.println("request " + this + " sends event " + doubleState);
                doubleEvent.trigger(new DoubleEvent(doubleState));
                System.out.println("request " + this + " returns " + future.get());
                return future.get();
            } else {
                System.out.println("request " + this + " returns " + doubleState);
                return doubleState;
            }
        }

        @In
        private void onDouble(DoubleEvent event) {
            double oldState = doubleState;
            doubleState /= event.getData() + 0.5;
            System.out.println("event " + this + " receives " + event.getData() + ", old state = " + oldState + " new state = " + doubleState);
        }

        void reset() {
            doubleState = 1.0;
        }
    }

    static class Fixture {

        Random random;
        Component[] components;
        int[] randomSequence;
        int seqIdx = 0;

        Fixture(long seed, int numberOfComponents) {
            random = new Random(seed);
            components = new Component[numberOfComponents];

            for (int i = 0; i < components.length; i++) {
                components[i] = new Component();
            }

            randomSequence = random.ints(0, components.length)
                    .limit(1000)
                    .toArray();
        }

        int next() {
            return randomSequence[seqIdx++];
        }

        void reset() {
            seqIdx = 0;

            for (int i = 0; i < components.length; i++) {
                components[i].reset();
            }
        }
    }

    @BeforeEach
    public void beforeEach() {
        Ports.releaseDomains();
    }

    @Test
    public void nestedDeadlock() {
        Component a = new AsyncTest.Component();
        Component b = new AsyncTest.Component();
        Component c = new AsyncTest.Component();

        Ports.connectDirected(a, b, PortsOptions.FORCE_CONNECT_ALL);
        Ports.connectDirected(b, c, PortsOptions.FORCE_CONNECT_ALL);

        double expectedA = a.doubleRequest.call(new DoubleRequest(40));
        double expectedB = b.doubleRequest.call(new DoubleRequest(50));
//        double expectedC = c.doubleRequest.call(new DoubleRequest(1));

        Domain d0 = Ports.domain("d0", DispatchPolicy.ASYNCHRONOUS, SyncPolicy.DOMAIN);
        Domain d1 = Ports.domain("d1", DispatchPolicy.ASYNCHRONOUS, SyncPolicy.DOMAIN);
        Domain d2 = Ports.domain("d2", DispatchPolicy.ASYNCHRONOUS, SyncPolicy.DOMAIN);

        d0.addInstances(a);
        d1.addInstances(b);
        d2.addInstances(c);

        a.reset();
        b.reset();
        c.reset();

        System.out.println();

        double actualA = a.doubleRequest.call(new DoubleRequest(40));
        double actualB = b.doubleRequest.call(new DoubleRequest(50));
//        double actualC = c.doubleRequest.call(new DoubleRequest(1));

//        assertEquals(expectedA, actualA);
        assertEquals(expectedB, actualB);
//        assertEquals(expectedC, actualC);
    }

    @Test
    public void asyncRandomized01() {
        f(new Fixture(0L, NUMBER_OF_COMPONENTS));
    }

    @Test
    public void asyncRandomized02() {
        f(new Fixture(1L, NUMBER_OF_COMPONENTS));
    }

    @Test
    public void asyncRandomized03() {
        f(new Fixture(2L, NUMBER_OF_COMPONENTS));
    }

    @Test
    public void asyncRandomized04() {
        f(new Fixture(3L, NUMBER_OF_COMPONENTS));
    }

    @Test
    public void asyncRandomized05() {
        f(new Fixture(4L, NUMBER_OF_COMPONENTS));
    }

    @Test
    public void asyncRandomized06() {
        f(new Fixture(5L, NUMBER_OF_COMPONENTS));
    }

    @Test
    public void asyncRandomized07() {
        f(new Fixture(6L, NUMBER_OF_COMPONENTS));
    }

    @Test
    public void asyncRandomized08() {
        f(new Fixture(7L, NUMBER_OF_COMPONENTS));
    }

    @Test
    public void asyncRandomized09() {
        f(new Fixture(8L, NUMBER_OF_COMPONENTS));
    }

    @Test
    public void asyncRandomized10() {
        f(new Fixture(9L, NUMBER_OF_COMPONENTS));
    }

    private void f(Fixture fixture) {
        for (int i = 0; i < fixture.components.length; i++) {
            int numberOfConnections = fixture.next() % 2 + 1;

            for (int j = 0; j < numberOfConnections; j++) {
                int connectTo;

                do {
                    connectTo = fixture.next();
                } while (connectTo == i);

                Ports.connectDirected(fixture.components[i], fixture.components[connectTo], PortsOptions.FORCE_CONNECT_EVENT_PORTS);
            }
        }

        List<Double> expected = r(fixture);

        System.out.println();
        System.out.println();

        Domain d0 = Ports.domain("d0", DispatchPolicy.SYNCHRONOUS, SyncPolicy.DOMAIN);
        Domain d1 = Ports.domain("d1", DispatchPolicy.ASYNCHRONOUS, SyncPolicy.COMPONENT);
        Domain d2 = Ports.domain("d2", DispatchPolicy.ASYNCHRONOUS, SyncPolicy.DOMAIN);
        Domain d3 = Ports.domain("d3", DispatchPolicy.ASYNCHRONOUS, SyncPolicy.COMPONENT);
        Domain d4 = Ports.domain("d4", DispatchPolicy.ASYNCHRONOUS, SyncPolicy.DOMAIN);

        for (int i = 0; i < fixture.components.length; i++) {
            if (i < fixture.components.length / 5) {
                d0.addInstances(fixture.components[i]);
            } else if (i < 2 * fixture.components.length / 5) {
                d1.addInstances(fixture.components[i]);
            } if (i < 3 * fixture.components.length / 5) {
                d2.addInstances(fixture.components[i]);
            } if (i < 4 * fixture.components.length / 5) {
                d3.addInstances(fixture.components[i]);
            }  else {
                d4.addInstances(fixture.components[i]);
            }
        }

        Executor.BREAKPOINT_ENABLE = true;

        List<Double> actual = r(fixture);

        assertIterableEquals(expected, actual);
    }

    private List<Double> r(Fixture fixture) {
        fixture.reset();
        List<Double> results = new ArrayList<>();

        for (int i = 0; i < fixture.components.length; i++) {
            fixture.components[i].doubleEvent.trigger(new DoubleEvent(fixture.next()));
            double result = fixture.components[i].doubleRequest.call(new DoubleRequest(fixture.next()/3));
            results.add(result);
        }

        return results;
    }
}
