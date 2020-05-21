package org.timux.ports;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;

public class AsyncTest {

    private static final int NUMBER_OF_COMPONENTS = 20;

    static class Component {

        private Double doubleState;

        Component() {
            reset();
        }

        @Out
        private Request<DoubleRequest, Double> doubleRequest;

        @Out
        private Event<DoubleEvent> doubleEvent;

        @In
        private Double onDoubleRequest(DoubleRequest request) {
            doubleState *= request.getData() + 0.5;

//            System.out.println("request " + this + " " + request.getData());

            if (request.getData() > 0) {
                PortsFuture<Double> future = doubleRequest.submit(new DoubleRequest(request.getData() - 1));
//                doubleEvent.trigger(new DoubleEvent(doubleState));
                return future.get();
            } else {
                return doubleState;
            }
        }

        @In
        private void onDouble(DoubleEvent event) {
//            System.out.println("event " + this + " " + event.getData());
            doubleState /= event.getData() + 0.5;
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
            double result = fixture.components[i].doubleRequest.call(new DoubleRequest(fixture.next()));
            results.add(result);
        }

        return results;
    }
}
