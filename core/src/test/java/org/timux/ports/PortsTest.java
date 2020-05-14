/*
 * Copyright 2018-2020 Tim Rohlfs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package org.timux.ports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.timux.ports.testapp.component.IntEvent;
import org.timux.ports.types.*;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class PortsTest {

    @BeforeEach
    public void setupEach() {
        Ports.releaseProtocols();
    }

    @Test
    public void smokeTest() {
        A a = new A();
        B b = new B();

        Ports.connect(a).and(b);

        a.intEvent.trigger(new IntEvent(3));

        assertEquals(4.5, b.receivedData);
    }

    @Test
    public void multipleReceiversWithoutReconnection() {
        A a = new A();
        C c1 = new C();
        C c2 = new C();

        Ports.connect(a).and(c1);
        Ports.connect(a).and(c2);

        a.intEvent.trigger(new IntEvent(3));

        assertEquals(3, c1.data);
        assertEquals(0, c2.data);
    }

    @Test
    public void multipleReceiversWithReconnection() {
        A a = new A();
        C c1 = new C();
        C c2 = new C();

        Ports.connect(a).and(c1);
        Ports.connect(a).and(c2, PortsOptions.FORCE_CONNECT_EVENT_PORTS);

        a.intEvent.trigger(new IntEvent(3));

        assertEquals(3, c1.data);
        assertEquals(c1.data, c2.data);
    }

    @Test
    public void disconnect() {
        A a = new A();
        C c1 = new C();
        C c2 = new C();

        Ports.connect(a).and(c1);
        Ports.connect(a).and(c2, PortsOptions.FORCE_CONNECT_EVENT_PORTS);

        Ports.disconnect(a).and(c1);

        a.intEvent.trigger(new IntEvent(3));

        assertEquals(0, c1.data);
        assertEquals(3, c2.data);
    }

    @Test
    public void disconnectAll() {
        A a = new A();
        C c1 = new C();
        C c2 = new C();

        Ports.connect(a).and(c1);
        Ports.connect(a).and(c2, PortsOptions.FORCE_CONNECT_EVENT_PORTS);

        Ports.disconnect(a, c1);

        a.intEvent.trigger(new IntEvent(3));

        assertEquals(0, c1.data);
        assertEquals(3, c2.data);
    }

    @Test
    public void protocolsSmokeTest() {
        A a = new A();
        B b = new B();

        Ports.connect(a).and(b);

        Ports.register(a, b);

        ValueContainer<Boolean> firstActionA = new ValueContainer<>(false);
        ValueContainer<Boolean> secondActionA = new ValueContainer<>(false);
        ValueContainer<Boolean> firstActionB = new ValueContainer<>(false);
        ValueContainer<Boolean> secondActionB = new ValueContainer<>(false);
        ValueContainer<Boolean> firstActionC = new ValueContainer<>(false);
        ValueContainer<Boolean> firstActionD = new ValueContainer<>(false);

        ValueContainer<Integer> doubleRequestIndex = new ValueContainer<>(0);
        double[] doubleRequestValues = {50.0, 2.5, 2.0, 2.5, 4.0};

        ValueContainer<Boolean> doubleRequestResponse = new ValueContainer<>(false);

        Ports.protocol()
            .when(IntEvent.class)
                .triggers(x -> x.getData() > 1)
                    .do_((x, owner) ->
                            firstActionA.value = (!firstActionA.value && x.getData() == 4 && owner == a)
                                    || (firstActionA.value && x.getData() == 2 && owner != a))
                    .do_((x, owner) ->
                            secondActionA.value = (!secondActionA.value && x.getData() == 4 && owner == a) ||
                                    (secondActionA.value && x.getData() == 2 && owner != a))
            .when(IntEvent.class)
                .triggers(x -> x.getData() > 2)
                    .do_((x, owner) -> firstActionB.value = !firstActionB.value && x.getData() == 4 && owner == a)
                    .do_((x, owner) -> secondActionB.value = !secondActionB.value && x.getData() == 4 && owner == a);

        Ports.protocol()
            .when(IntEvent.class)
                .triggers(x -> x.getData() > 3)
                    .do_((x, owner) -> firstActionC.value = !firstActionC.value && x.getData() == 4 && owner == a)
                    .with(DoubleRequest.class, Double.class)
                        .call(new DoubleRequest(50.0))
                    .with(IntEvent.class)
                        .trigger(new IntEvent(2))
            .when(DoubleRequest.class, Double.class)
                .requests(x -> x.getData() >= 4.0)
                    .do_((x, owner) ->
                            firstActionD.value = (!firstActionD.value && x.getData() == 50.0 && owner != b)
                                    || (firstActionD.value && x.getData() == 4.0 && owner == b))
            .when(DoubleRequest.class, Double.class)
                .requests(x -> x.getData() > 3.0)
                    .respond(17.5)
            .when(DoubleRequest.class, Double.class)
                .responds(x -> x > 5.0)
                    .do_((x, owner) ->
                            doubleRequestResponse.value = (!doubleRequestResponse.value && x == 17.5 && owner != b)
                                    || (doubleRequestResponse.value && x == 17.5 && owner == b));

        Ports.protocol()
            .when(IntEvent.class)
                .triggers(x -> x.getData() > 1)
                .with(DoubleRequest.class, Double.class, b)
                    .call(new DoubleRequest(2.5))
            .when(DoubleRequest.class, Double.class)
                .requests()
                    .do_(x -> {
                        if (x.getData() == doubleRequestValues[doubleRequestIndex.value]) {
                            doubleRequestIndex.value++;
                        } else {
                            System.out.println(x.getData() );
                            System.out.println(doubleRequestValues[doubleRequestIndex.value]);
                            System.out.println(x.getData() - doubleRequestValues[doubleRequestIndex.value]);
                            System.out.println();
                        }
                    });

        Ports.protocol()
            .with(IntEvent.class, a)
                .trigger(new IntEvent(4));

        Ports.awaitQuiescence();

        assertTrue(firstActionA.value);
        assertTrue(secondActionA.value);
        assertTrue(firstActionB.value);
        assertTrue(secondActionB.value);
        assertTrue(firstActionC.value);
        assertTrue(firstActionD.value);
        assertEquals(5, doubleRequestIndex.value.intValue());
        assertTrue(doubleRequestResponse.value);
    }

    @Test
    public void protocolsRawUnionTypeExceptionTest() {
        assertThrows(RawUnionTypeException.class, () -> {
            Ports.protocol()
                .when(EitherRequest.class, Either.class)
                    .requests()
                        .respond(request -> Either.a(request.getValue()));
        });

        assertThrows(RawUnionTypeException.class, () -> {
            Ports.protocol()
                .with(EitherRequest.class, Either.class)
                    .call(new EitherRequest(1.0));
        });
    }

    @Test
    public void protocolsEitherTest() {
        D d = new D();

        Ports.register(d);

        ValueContainer<Either<Double, String>> eitherValue = new ValueContainer<>(null);

        Ports.protocol()
            .when(EitherRequest.class, Double.class, String.class)
                .requests()
                    .respond(request -> Either.a(request.getValue()))
            .when(EitherRequest.class, Double.class, String.class)
                .responds()
                    .do_(response -> eitherValue.value = response);

        Ports.protocol()
            .with(EitherRequest.class, Double.class, String.class)
                .call(new EitherRequest(1.0));

        assertNotNull(eitherValue.value);
        assertEquals(1.0, eitherValue.value.map(x -> x, Double::parseDouble), 0.0);
    }

    @Test
    public void protocolsEither3Test() {
        D d = new D();

        Ports.register(d);

        ValueContainer<Either3<Double, Integer, String>> eitherValue = new ValueContainer<>(null);

        Ports.protocol()
            .when(Either3Request.class, Double.class, Integer.class, String.class)
                .requests()
                    .respond(request -> Either3.b(request.getValue()))
            .when(Either3Request.class, Double.class, Integer.class, String.class)
                .responds()
                    .do_(response -> eitherValue.value = response);

        Ports.protocol()
            .with(Either3Request.class, Double.class, Integer.class, String.class)
                .call(new Either3Request(1));

        assertNotNull(eitherValue.value);
        assertEquals(1.0, eitherValue.value.map(x -> x, x -> (double) x, Double::parseDouble), 0.0);
    }

    @Test
    public void gcWithEvents() {
        A a = new A();
        B b = new B();

        Ports.connect(a).and(b);
        a.intEvent.trigger(new IntEvent(3));
        assertEquals(4.5, b.receivedData);

        b = null;

        System.gc();

        a.intEvent.trigger(new IntEvent(8));
        assertEquals(3.0, a.receivedData);

        b = new B();

        Ports.connect(a).and(b);
        a.intEvent.trigger(new IntEvent(10));
        assertEquals(10.0, a.receivedData);
        assertEquals(15.0, b.receivedData);
    }

    @Test
    public void forkGet() {
        A a = new A();
        B b = new B();

        Ports.connect(a).and(b);

        Ports.domain("test-a", SyncPolicy.NO_SYNC, DispatchPolicy.PARALLEL)
                .addComponents(a);

        Ports.domain("test-b", SyncPolicy.NO_SYNC, DispatchPolicy.PARALLEL)
                .addComponents(b);

        Fork<Double> fork = b.doubleRequest.fork(10, DoubleRequest::new);

        List<Either<Double, Throwable>> results = fork.getEither();

        assertEquals(10, results.size());

        for (int i = 0; i < results.size(); i++) {
            int finalI = i;

            results.get(i).on(
                    value -> assertEquals(finalI * 1.5, value),
                    throwable -> fail("index " + finalI + ": request should not fail: " + throwable)
            );
        }
    }

    @Test
    public void forkGetNow() {
        Executor.TEST_API_MAX_NUMBER_OF_THREADS = 10;

        A a = new A();
        B b = new B();

        Ports.domain("test-a", SyncPolicy.NO_SYNC, DispatchPolicy.PARALLEL)
                .addComponents(a);

        Ports.domain("test-b", SyncPolicy.NO_SYNC, DispatchPolicy.PARALLEL)
                .addComponents(b);

        Ports.connect(a).and(b);

        Fork<Double> fork = b.slowRequest.fork(10, SlowRequest::new);

        List<Either3<Double, Nothing, Throwable>> results = fork.getNowEither();

        assertEquals(10, results.size());

        for (int i = 0; i < results.size(); i++) {
            int finalI = i;

            results.get(i).on(
                    value -> fail("index " + finalI + ": there should be no result available (" + value + ")"),
                    nothing -> {},
                    throwable -> fail("index " + finalI + ": request should not fail: ", throwable)
            );
        }

        try {
            Thread.sleep(600);
        } catch (InterruptedException e) {
            fail(e);
        }

        results = fork.getNowEither();

        assertEquals(10, results.size());

        for (int i = 0; i < results.size(); i++) {
            int finalI = i;

            results.get(i).on(
                    value -> assertTrue(finalI < 5, "index " + finalI),
                    nothing -> assertTrue(finalI >= 5, "index " + finalI),
                    throwable -> fail("index " + finalI + ": request should not fail: ", throwable)
            );
        }

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            fail(e);
        }

        results = fork.getNowEither();

        assertEquals(10, results.size());

        for (int i = 0; i < results.size(); i++) {
            int finalI = i;

            results.get(i).on(
                    value -> {},
                    nothing -> fail(finalI + ": there should be a result available"),
                    throwable -> fail("index " + finalI + ": request should not fail: ", throwable)
            );
        }
    }

    @Test
    public void threadIdleLifetime() {
        Executor.TEST_API_MAX_NUMBER_OF_THREADS = 10;
        Executor.TEST_API_IDLE_LIFETIME_MS = 2000;

        A a = new A();
        B b = new B();

        Ports.domain("test-a", SyncPolicy.NO_SYNC, DispatchPolicy.PARALLEL)
                .addComponents(a);

        Ports.domain("test-b", SyncPolicy.NO_SYNC, DispatchPolicy.PARALLEL)
                .addComponents(b);

        Ports.connect(a).and(b);

        Fork<Double> fork = b.slowRequest.fork(10, SlowRequest::new);
        fork.get();

        long startTime = System.currentTimeMillis();

        for (;;) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                fail(e);
            }

            Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();

            long numberOfAsyncThreads = threads.keySet().stream()
                    .filter(thread -> thread.getName().startsWith("ports-worker-"))
                    .count();

            long waitTime = System.currentTimeMillis() - startTime;

            if (waitTime > 1950) {
                assertEquals(1, numberOfAsyncThreads);
                break;
            } else if (waitTime > 1450) {
                assertEquals(5, numberOfAsyncThreads);
            } else {
                assertEquals(10, numberOfAsyncThreads);
            }
        }
    }

    @Test
    public void pairTripleEquals() {
        Triple<Integer, Integer, Integer> t1 = new Triple<>(1, 2, 3);
        Triple<Integer, Integer, Integer> t2 = new Triple<>(1, 2, 3);

        assertEquals(t1, t2);

        Triple<Integer, Integer, Integer> t3 = new Triple<>(0, 2, 3);
        Triple<Integer, Integer, Integer> t4 = new Triple<>(1, 0, 3);
        Triple<Integer, Integer, Integer> t5 = new Triple<>(1, 2, 0);

        assertNotEquals(t1, t3);
        assertNotEquals(t1, t4);
        assertNotEquals(t1, t5);

        Pair<Integer, Integer> p1 = new Pair<>(1, 2);
        Pair<Integer, Integer> p2 = new Pair<>(1, 2);

        assertEquals(p1, p2);

        Pair<Integer, Integer> p3 = new Pair<>(0, 2);
        Pair<Integer, Integer> p4 = new Pair<>(1, 0);

        assertNotEquals(p1, p3);
        assertNotEquals(p1, p4);
    }

    @Test
    public void pairTripleContains() {
        Triple<Integer, Integer, Integer> t1 = new Triple<>(1, 2, 3);

        Pair<Integer, Integer> p1 = new Pair<>(1, 2);
        Pair<Integer, Integer> p2 = new Pair<>(1, 3);
        Pair<Integer, Integer> p3 = new Pair<>(2, 3);
        Pair<Integer, Integer> p4 = new Pair<>(2, 1);
        Pair<Integer, Integer> p5 = new Pair<>(3, 1);
        Pair<Integer, Integer> p6 = new Pair<>(3, 2);

        assertTrue(t1.containsDistinct(p1));
        assertTrue(t1.containsDistinct(p2));
        assertTrue(t1.containsDistinct(p3));
        assertTrue(t1.containsDistinct(p4));
        assertTrue(t1.containsDistinct(p5));
        assertTrue(t1.containsDistinct(p6));

        Pair<Integer, Integer> p7 = new Pair<>(0, 2);
        Pair<Integer, Integer> p8 = new Pair<>(1, 0);

        assertFalse(t1.containsDistinct(p7));

        Pair<Integer, Integer> p9 = new Pair<>(1, 1);
        Pair<Integer, Integer> p10 = new Pair<>(2, 2);
        Pair<Integer, Integer> p11 = new Pair<>(2, null);

        assertFalse(t1.containsDistinct(p9));
        assertFalse(t1.containsDistinct(p10));
        assertFalse(t1.containsDistinct(p11));
    }

    @Test
    public void pairTripleContainsRandomized() {
        Random random = new Random(0);

        for (int i = 0; i < 10000; i++) {
            Integer a = random.nextInt(3) > 0 ? random.nextInt(10) : null;
            Integer b = random.nextInt(3) > 0 ? random.nextInt(10) : null;
            Integer c = random.nextInt(3) > 0 ? random.nextInt(10) : null;

            TripleX<Integer> triple = new TripleX<>(a, b, c);

            int x = random.nextInt(3);
            int y;

            do {
                y = random.nextInt(3);
            } while (y == x);

            boolean shallBeEqual = random.nextBoolean();

            if (shallBeEqual) {
                Pair<Integer, Integer> pair = new Pair<>(triple.get(x), triple.get(y));
                assertTrue(triple.containsDistinct(pair));
            } else {
                Pair<Integer, Integer> pair = new Pair<>(Integer.MAX_VALUE, triple.get(y));
                assertFalse(triple.containsDistinct(pair));

                pair = new Pair<>(triple.get(x), Integer.MAX_VALUE);
                assertFalse(triple.containsDistinct(pair));
            }
        }
    }
}
