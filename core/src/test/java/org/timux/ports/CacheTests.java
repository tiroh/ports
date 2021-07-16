/*
 * Copyright 2018-2021 Tim Rohlfs
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.timux.ports.types.Container;
import org.timux.ports.types.Either;
import org.timux.ports.types.Failure;

import static org.junit.jupiter.api.Assertions.*;

public class CacheTests {


    @AfterEach
    public void afterEach() {
        Ports.reset();
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
        assertSame(responseA, responseB);

        assertEquals(4 * 17, responseC.getAOrThrow());
        assertSame(responseC, responseD);

        assertNotSame(responseA, responseC);

        assertEquals("is negative: -4", responseE.getBOrThrow().getMessage());
        assertEquals("is negative: -4", responseF.getBOrThrow().getMessage());
        assertNotSame(responseE, responseF);
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
        assertSame(responseA, responseB);

        assertEquals(4 * 17, responseC.getAOrThrow());
        assertSame(responseC, responseD);

        assertNotSame(responseA, responseC);

        assertEquals("is negative: -4", responseE.getBOrThrow().getMessage());
        assertEquals("is negative: -4", responseF.getBOrThrow().getMessage());
        assertNotSame(responseE, responseF);
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
        assertSame(responseA, responseB);

        assertEquals(4 * 17, responseC.getAOrThrow());
        assertSame(responseC, responseD);

        assertNotSame(responseA, responseC);

        assertEquals("is negative: -4", responseE.getBOrThrow().getMessage());
        assertEquals("is negative: -4", responseF.getBOrThrow().getMessage());
        assertNotSame(responseE, responseF);
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
        assertSame(responseA, responseB);

        assertEquals(4 * 17, responseC.getAOrThrow());
        assertSame(responseC, responseD);

        assertNotSame(responseA, responseC);

        assertEquals("is negative: -4", responseE.getBOrThrow().getMessage());
        assertEquals("is negative: -4", responseF.getBOrThrow().getMessage());
        assertNotSame(responseE, responseF);
    }

    @Test
    public void statelessRequest() {
        PureSender pureSender = new PureSender();
        PureReceiver pureReceiver = new PureReceiver();

        Ports.connect(pureSender).and(pureReceiver);

        int response = pureSender.runStatelessRequest();

        assertEquals(17, response);
    }

    @Test
    public void clearCache() {
        PureSender pureSender = new PureSender();
        PureReceiver pureReceiver = new PureReceiver();

        Ports.connect(pureSender).and(pureReceiver);

        Either<Integer, Failure> responseA = pureSender.runCall(3);
        Either<Integer, Failure> responseB = pureSender.runCall(3);

        Ports.protocol()
            .with(ClearEvent.class)
                .trigger(new ClearEvent());

        Either<Integer, Failure> responseC = pureSender.runCall(3);
        Either<Integer, Failure> responseD = pureSender.runCall(3);

        Ports.protocol()
            .when(PureStatelessRequest.class, Integer.class)
                .requests()
                .respond(17);

        Either<Integer, Failure> responseE = pureSender.runCall(3);
        Either<Integer, Failure> responseF = pureSender.runCall(3);

        Ports.protocol()
            .with(PureStatelessRequest.class, Integer.class)
                .call(new PureStatelessRequest());

        Either<Integer, Failure> responseG = pureSender.runCall(3);
        Either<Integer, Failure> responseH = pureSender.runCall(3);

        assertEquals(3 * 17, responseA.getAOrThrow());
        assertSame(responseA, responseB);

        assertEquals(3 * 17, responseC.getAOrThrow());
        assertSame(responseC, responseD);

        assertEquals(3 * 17, responseE.getAOrThrow());
        assertSame(responseE, responseF);

        assertEquals(3 * 17, responseG.getAOrThrow());
        assertSame(responseG, responseH);

        assertNotSame(responseA, responseC);
        assertNotSame(responseA, responseE);
        assertNotSame(responseA, responseF);

        assertSame(responseC, responseE);
        assertSame(responseC, responseF);

        assertNotSame(responseE, responseG);
        assertSame(responseE, responseF);
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
