package org.timux.ports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.timux.ports.types.Container;
import org.timux.ports.types.Either;
import org.timux.ports.types.Failure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CacheTests {

    @AfterEach
    public void afterEach() {
        Ports.releaseProtocols();
    }

    @Test
    public void requestCache() {
        RequestCache<Integer, Integer> requestCache = new RequestCache<>(8, Integer.class);

        for (int i = 0; i < 16; i++) {
            requestCache.put(i, 2 * i);
        }

        for (int i = 0; i < 8; i++) {
            assertNull(requestCache.get(i));
        }

        for (int i = 8; i < 16; i++) {
            assertEquals(2 * i, requestCache.get(i));
        }

        for (int i = 0; i < 5; i++) {
            requestCache.put(i, 2 * i);
        }

        for (int i = 0; i < 5; i++) {
            assertEquals(2 * i, requestCache.get(i));
        }
    }

    @Test
    public void constEitherRequestWithCall() {
        PureSender pureSender = new PureSender();
        PureReceiver pureReceiver = new PureReceiver();

        Ports.connect(pureSender).and(pureReceiver);

        Either<Integer, Failure> responseA = pureSender.runCall(3);
        Either<Integer, Failure> responseB = pureSender.runCall(3);

        Either<Integer, Failure> responseC = pureSender.runCall(4);
        Either<Integer, Failure> responseD = pureSender.runCall(4);

        Either<Integer, Failure> responseE = pureSender.runCall(-4);
        Either<Integer, Failure> responseF = pureSender.runCall(-4);

        assertEquals(3 * 17, responseA.getAOrThrow());
        assertEquals(responseA, responseB);

        assertEquals(4 * 17, responseC.getAOrThrow());
        assertEquals(responseC, responseD);

        assertNotEquals(responseA, responseC);

        assertEquals("is negative: -4", responseE.getBOrThrow().getMessage());
        assertEquals("is negative: -4", responseF.getBOrThrow().getMessage());
        assertNotEquals(responseE, responseF);
    }

    @Test
    public void constEitherRequestWithCallE() {
        PureSender pureSender = new PureSender();
        PureReceiver pureReceiver = new PureReceiver();

        Ports.connect(pureSender).and(pureReceiver);

        Either<Integer, Failure> responseA = pureSender.runCallE(3);
        Either<Integer, Failure> responseB = pureSender.runCallE(3);

        Either<Integer, Failure> responseC = pureSender.runCallE(4);
        Either<Integer, Failure> responseD = pureSender.runCallE(4);

        Either<Integer, Failure> responseE = pureSender.runCallE(-4);
        Either<Integer, Failure> responseF = pureSender.runCallE(-4);

        assertEquals(3 * 17, responseA.getAOrThrow());
        assertEquals(responseA, responseB);

        assertEquals(4 * 17, responseC.getAOrThrow());
        assertEquals(responseC, responseD);

        assertNotEquals(responseA, responseC);

        assertEquals("is negative: -4", responseE.getBOrThrow().getMessage());
        assertEquals("is negative: -4", responseF.getBOrThrow().getMessage());
        assertNotEquals(responseE, responseF);
    }

    @Test
    public void constEitherRequestWithCallF() {
        PureSender pureSender = new PureSender();
        PureReceiver pureReceiver = new PureReceiver();

        Ports.connect(pureSender).and(pureReceiver);

        Either<Integer, Failure> responseA = pureSender.runCallF(3);
        Either<Integer, Failure> responseB = pureSender.runCallF(3);

        Either<Integer, Failure> responseC = pureSender.runCallF(4);
        Either<Integer, Failure> responseD = pureSender.runCallF(4);

        Either<Integer, Failure> responseE = pureSender.runCallF(-4);
        Either<Integer, Failure> responseF = pureSender.runCallF(-4);

        assertEquals(3 * 17, responseA.getAOrThrow());
        assertEquals(responseA, responseB);

        assertEquals(4 * 17, responseC.getAOrThrow());
        assertEquals(responseC, responseD);

        assertNotEquals(responseA, responseC);

        assertEquals("is negative: -4", responseE.getBOrThrow().getMessage());
        assertEquals("is negative: -4", responseF.getBOrThrow().getMessage());
        assertNotEquals(responseE, responseF);
    }

    @Test
    public void constEitherProtocolRequest() {
        PureSender pureSender = new PureSender();

        // Ensure port instantiation.
        Ports.connect(pureSender).and(pureSender);

        Container<Integer> responseCounter = Container.of(0);

        Ports.protocol()
            .when(PureEitherRequest.class, Integer.class, Failure.class)
                .responds()
                .do_(() -> responseCounter.value++)
            .when(PureEitherRequest.class, Integer.class, Failure.class)
                .requests()
                .respond(request -> request.getArg() >= 0
                        ? Either.a(request.getArg() * 17)
                        : Either.failure("is negative: " + request.getArg()));

        Either<Integer, Failure> responseA = pureSender.runCall(3);
        Either<Integer, Failure> responseB = pureSender.runCall(3);

        Either<Integer, Failure> responseC = pureSender.runCall(4);
        Either<Integer, Failure> responseD = pureSender.runCall(4);

        Either<Integer, Failure> responseE = pureSender.runCall(-4);
        Either<Integer, Failure> responseF = pureSender.runCall(-4);

        assertEquals(6, responseCounter.value);

        assertEquals(3 * 17, responseA.getAOrThrow());
        assertEquals(responseA, responseB);

        assertEquals(4 * 17, responseC.getAOrThrow());
        assertEquals(responseC, responseD);

        assertNotEquals(responseA, responseC);

        assertEquals("is negative: -4", responseE.getBOrThrow().getMessage());
        assertEquals("is negative: -4", responseF.getBOrThrow().getMessage());
        assertNotEquals(responseE, responseF);
    }

//    @Test
//    public void requestCachePerformance() {
//        RequestCache<Integer, Integer> requestCache = new RequestCache<>(101);
//
//        ArrayList<Integer> list = new ArrayList<>();
//
//        Collections.shuffle(
//                IntStream.range(0, 101).collect(() -> list, ArrayList::add, ArrayList::addAll));
//
//        list.forEach(i -> requestCache.put(i, 2 * i));
//
//        Random random = new Random(1L);
//
//        float c = 0;
//        int s = 0;
//
//        for (int i = 0; i < 10000; i++) {
//            int v = (list.size() >> 1) + (int) (random.nextGaussian() * 10.0);
//
//            if (v < 0 || v >= list.size()) {
//                continue;
//            }
//
//            requestCache.get(v);
//            s += requestCache.steps;
//
//            if (++c == 200) {
//                System.out.println(s / c);
//                c = 0;
//                s = 0;
//            }
//        }
//    }
}
