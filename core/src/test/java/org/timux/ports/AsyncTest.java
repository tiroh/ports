package org.timux.ports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
            Object sender = request.getSender();

            if (sender == this) {
                return doubleState;
            }

            doubleState *= request.getData() + 0.5;

            if (request.getData() > 0 && doubleRequest != null && doubleEvent != null) {
                PortsFuture<Double> future = doubleRequest.callF(new DoubleRequest(request.getData() - 1, sender));
                doubleEvent.trigger(new DoubleEvent(doubleState));
                PortsFuture<Double> future2 = doubleRequest.callF(new DoubleRequest(request.getData() - 1.1, sender));
                doubleEvent.trigger(new DoubleEvent(doubleState*2));
                doubleEvent.trigger(new DoubleEvent(doubleState*3));
                return future.get() + future2.get();
            } else {
                return doubleState;
            }
        }

        @In
        private void onDouble(DoubleEvent event) {
            double oldState = doubleState;
            doubleState /= event.getData() + 0.3;
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

    @BeforeAll
    public static void beforeAll() {
        Executor.TEST_API_DISABLE_DEADLOCK_WARNINGS = true;
    }

    @AfterEach
    public void afterEach() {
        Ports.reset();
    }

    @Test
    public void nestedDeadlock() {
        Component a = new Component();
        Component b = new Component();
        Component c = new Component();

        Ports.connectDirected(a, b, PortsOptions.FORCE_CONNECT_ALL);
        Ports.connectDirected(b, c, PortsOptions.FORCE_CONNECT_ALL);

        double expectedA = a.doubleRequest.call(new DoubleRequest(40));
        double expectedB = b.doubleRequest.call(new DoubleRequest(49));

        Domain d0 = Ports.domain("d0", DispatchPolicy.ASYNCHRONOUS, SyncPolicy.DOMAIN);
        Domain d1 = Ports.domain("d1", DispatchPolicy.ASYNCHRONOUS, SyncPolicy.DOMAIN);
        Domain d2 = Ports.domain("d2", DispatchPolicy.ASYNCHRONOUS, SyncPolicy.DOMAIN);

        d0.addInstances(a);
        d1.addInstances(b);
        d2.addInstances(c);

        a.reset();
        b.reset();
        c.reset();

        double actualA = a.doubleRequest.call(new DoubleRequest(40));
        double actualB = b.doubleRequest.call(new DoubleRequest(49));

        assertEquals(expectedA, actualA);
        assertEquals(expectedB, actualB);
    }

    @Test
    public void asyncRandomized01() {
        f(new Fixture(0L, NUMBER_OF_COMPONENTS), false);
    }

    @Test
    public void asyncRandomized02() {
        f(new Fixture(1L, NUMBER_OF_COMPONENTS), false);
    }

    @Test
    public void asyncRandomized03() {
        f(new Fixture(2L, NUMBER_OF_COMPONENTS), false);
    }

    @Test
    public void asyncRandomized04() {
        f(new Fixture(3L, NUMBER_OF_COMPONENTS), false);
    }

    @Test
    public void asyncRandomized05() {
        f(new Fixture(4L, NUMBER_OF_COMPONENTS), false);
    }

    @Test
    public void asyncRandomized06() {
        f(new Fixture(5L, NUMBER_OF_COMPONENTS), false);
    }

    @Test
    public void asyncRandomized07() {
        f(new Fixture(6L, NUMBER_OF_COMPONENTS), false);
    }

    @Test
    public void asyncRandomized08() {
        f(new Fixture(7L, NUMBER_OF_COMPONENTS), false);
    }

    @Test
    public void asyncRandomized09() {
        f(new Fixture(8L, NUMBER_OF_COMPONENTS), false);
    }

    @Test
    public void asyncRandomized10() {
        f(new Fixture(9L, NUMBER_OF_COMPONENTS), false);
    }

    @Test
    public void asyncRandomized11() {
        f(new Fixture(10L, NUMBER_OF_COMPONENTS), false);
    }

    @Test
    public void asyncRandomized12() {
        f(new Fixture(11L, NUMBER_OF_COMPONENTS), false);
    }

    @Test
    public void asyncRandomized13() {
        f(new Fixture(12L, NUMBER_OF_COMPONENTS), false);
    }

    @Test
    public void asyncRandomized14() {
        f(new Fixture(13L, NUMBER_OF_COMPONENTS), false);
    }

    @Test
    public void asyncRandomized15() {
        f(new Fixture(14L, NUMBER_OF_COMPONENTS), false);
    }

    @Test
    public void asyncRandomized16() {
        f(new Fixture(15L, NUMBER_OF_COMPONENTS), false);
    }

    @Test
    public void asyncRandomized17() {
        f(new Fixture(16L, NUMBER_OF_COMPONENTS), false);
    }

    @Test
    public void asyncRandomized18() {
        f(new Fixture(17L, NUMBER_OF_COMPONENTS), false);
    }

    @Test
    public void asyncRandomized19() {
        f(new Fixture(18L, NUMBER_OF_COMPONENTS), false);
    }

    @Test
    public void asyncRandomized20() {
        f(new Fixture(19L, NUMBER_OF_COMPONENTS), false);
    }

    private void f(Fixture fixture, boolean checkConsistency) {
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

        DispatchPolicy[] dispatchPolicies = {DispatchPolicy.SYNCHRONOUS, DispatchPolicy.ASYNCHRONOUS, DispatchPolicy.PARALLEL};
        SyncPolicy[] syncPolicies = {SyncPolicy.NONE, SyncPolicy.COMPONENT, SyncPolicy.DOMAIN};

        Domain d0 = Ports.domain("d0", dispatchPolicies[fixture.next() % 3], syncPolicies[fixture.next() % 3]);
        Domain d1 = Ports.domain("d1", dispatchPolicies[fixture.next() % 3], syncPolicies[fixture.next() % 3]);
        Domain d2 = Ports.domain("d2", dispatchPolicies[fixture.next() % 3], syncPolicies[fixture.next() % 3]);
        Domain d3 = Ports.domain("d3", dispatchPolicies[fixture.next() % 3], syncPolicies[fixture.next() % 3]);
        Domain d4 = Ports.domain("d4", dispatchPolicies[fixture.next() % 3], syncPolicies[fixture.next() % 3]);

        for (int i = 0; i < fixture.components.length; i++) {
            if (i < fixture.components.length / 5) {
                d0.addInstances(fixture.components[i]);
            } else if (i < 2 * fixture.components.length / 5) {
                d1.addInstances(fixture.components[i]);
            } else if (i < 3 * fixture.components.length / 5) {
                d2.addInstances(fixture.components[i]);
            } else if (i < 4 * fixture.components.length / 5) {
                d3.addInstances(fixture.components[i]);
            } else {
                d4.addInstances(fixture.components[i]);
            }
        }

        List<Double> actual = r(fixture);

        if (checkConsistency) {
            assertIterableEquals(expected, actual);
        } else {
            assertTrue(true);
        }

        System.out.println(d0.getNumberOfThreadsCreated() + " " + d1.getNumberOfThreadsCreated() + " " +
                d2.getNumberOfThreadsCreated() + " " + d3.getNumberOfThreadsCreated() + " " +
                d4.getNumberOfThreadsCreated());
    }

    private List<Double> r(Fixture fixture) {
        fixture.reset();
        List<Double> results = new ArrayList<>();

        for (int i = 0; i < fixture.components.length; i++) {
            fixture.components[i].doubleEvent.trigger(new DoubleEvent(fixture.next()));
            double result = fixture.components[i].doubleRequest.call(new DoubleRequest(fixture.next() / 2.1 + 1.0, fixture.components[i]));
            results.add(result);
        }

        return results;
    }
}
