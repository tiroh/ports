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

import org.junit.jupiter.api.Test;
import org.timux.ports.testapp.component.IntEvent;
import org.timux.ports.types.Pair;
import org.timux.ports.types.PairX;
import org.timux.ports.types.TripleX;
import org.timux.ports.types.Tuple;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleTests {

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
    public void pairTripleEquals() {
        TripleX<Integer> t1 = Tuple.ofX(1, 2, 3);
        TripleX<Integer> t2 = Tuple.ofX(1, 2, 3);

        assertEquals(t1, t2);

        TripleX<Integer> t3 = Tuple.ofX(0, 2, 3);
        TripleX<Integer> t4 = Tuple.ofX(1, 0, 3);
        TripleX<Integer> t5 = Tuple.ofX(1, 2, 0);

        assertNotEquals(t1, t3);
        assertNotEquals(t1, t4);
        assertNotEquals(t1, t5);

        Pair<Integer, Integer> p1 = Tuple.of(1, 2);
        Pair<Integer, Integer> p2 = Tuple.of(1, 2);

        assertEquals(p1, p2);

        Pair<Integer, Integer> p3 = Tuple.of(0, 2);
        Pair<Integer, Integer> p4 = Tuple.of(1, 0);

        assertNotEquals(p1, p3);
        assertNotEquals(p1, p4);
    }

    @Test
    public void pairTripleContains() {
        TripleX<Integer> t1 = Tuple.ofX(1, 2, 3);

        PairX<Integer> p1 = Tuple.ofX(1, 2);
        PairX<Integer> p2 = Tuple.ofX(1, 3);
        PairX<Integer> p3 = Tuple.ofX(2, 3);
        PairX<Integer> p4 = Tuple.ofX(2, 1);
        PairX<Integer> p5 = Tuple.ofX(3, 1);
        PairX<Integer> p6 = Tuple.ofX(3, 2);

        assertTrue(t1.containsDistinct(p1));
        assertTrue(t1.containsDistinct(p2));
        assertTrue(t1.containsDistinct(p3));
        assertTrue(t1.containsDistinct(p4));
        assertTrue(t1.containsDistinct(p5));
        assertTrue(t1.containsDistinct(p6));

        Pair<Integer, Integer> p7 = Tuple.of(0, 2);
        Pair<Integer, Integer> p8 = Tuple.of(1, 0);

        assertFalse(t1.containsDistinct(p7));
        assertFalse(t1.containsDistinct(p8));

        Pair<Integer, Integer> p9 = Tuple.of(1, 1);
        Pair<Integer, Integer> p10 = Tuple.of(2, 2);
        Pair<Integer, Integer> p11 = Tuple.of(2, null);

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

            TripleX<Integer> triple = Tuple.ofX(a, b, c);

            int x = random.nextInt(3);
            int y;

            do {
                y = random.nextInt(3);
            } while (y == x);

            boolean shallBeEqual = random.nextBoolean();

            if (shallBeEqual) {
                Pair<Integer, Integer> pair = Tuple.of(triple.get(x), triple.get(y));
                assertTrue(triple.containsDistinct(pair));
            } else {
                Pair<Integer, Integer> pair = Tuple.of(Integer.MAX_VALUE, triple.get(y));
                assertFalse(triple.containsDistinct(pair));

                pair = Tuple.of(triple.get(x), Integer.MAX_VALUE);
                assertFalse(triple.containsDistinct(pair));
            }
        }
    }

    @Test
    public void deadlockResolutionAsync() {
        DeadlockA a = new DeadlockA();
        DeadlockB b = new DeadlockB();

        Ports.connect(a).and(b);

        Ports.domain("test-a", DispatchPolicy.ASYNCHRONOUS, SyncPolicy.COMPONENT)
                .addInstances(a);

        Ports.domain("test-b", DispatchPolicy.ASYNCHRONOUS, SyncPolicy.COMPONENT)
                .addInstances(b);

        double response = a.doubleRequest.call(new DoubleRequest(4.0));
        assertEquals(0.0, response);

        response = a.doubleRequest.call(new DoubleRequest(8.0));
        assertEquals(0.0, response);
    }

    @Test
    public void deadlockResolutionParallel() {
        DeadlockA a = new DeadlockA();
        DeadlockB b = new DeadlockB();

        Ports.connect(a).and(b);

        Ports.domain("test-a", DispatchPolicy.PARALLEL, SyncPolicy.COMPONENT)
                .addInstances(a);

        Ports.domain("test-b", DispatchPolicy.PARALLEL, SyncPolicy.COMPONENT)
                .addInstances(b);

        double response = a.doubleRequest.call(new DoubleRequest(4.0));
        assertEquals(0.0, response);

        response = a.doubleRequest.call(new DoubleRequest(32.0));
        assertEquals(0.0, response);
    }
}
