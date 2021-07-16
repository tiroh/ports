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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.timux.ports.types.Failure;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleTests {

    @BeforeAll
    public static void beforeAll() {
        Executor.TEST_API_DISABLE_DEADLOCK_WARNINGS = true;
    }

    @AfterEach
    public void afterEach() {
        Ports.reset();
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
    public void getRootCauseAndFirstNonPortsException() {
        Throwable t2 = new MySpecialTestException("test2");
        Throwable t1 = new MySpecialTestException("test", new InvocationTargetException(new RuntimeException(t2)));

        Failure f1 = Failure.of(new PortsExecutionException(new PortsExecutionException(t1)));
        Failure f2 = Failure.of(t1);
        Failure f4 = Failure.of("test");

        assertEquals(t1, f1.getFirstNonPortsThrowable().orElse(null));
        assertEquals(t1, f2.getFirstNonPortsThrowable().orElse(null));

        assertFalse(f4.getFirstNonPortsThrowable().isPresent());

        assertEquals(t2, f1.getRootCause().orElse(null));
        assertEquals(t2, f2.getRootCause().orElse(null));
    }

    @Test
    public void deadlockResolutionSync() {
        DeadlockA a = new DeadlockA();
        DeadlockB b = new DeadlockB();

        Ports.connect(a).and(b);

        Ports.domain("a", DispatchPolicy.ASYNCHRONOUS, SyncPolicy.COMPONENT)
                .addInstances(a);

        Ports.domain("b", DispatchPolicy.SYNCHRONOUS, SyncPolicy.COMPONENT)
                .addInstances(b);

        double response = a.doubleRequest.call(new DoubleRequest(4.0));
        assertEquals(0.0, response);

        response = a.doubleRequest.call(new DoubleRequest(8.0));
        assertEquals(0.0, response);
    }

    @Test
    public void deadlockResolutionSyncSameDomain() {
        DeadlockA a = new DeadlockA();
        DeadlockB b = new DeadlockB();

        Ports.connect(a).and(b);

        Ports.domain("test", DispatchPolicy.ASYNCHRONOUS, SyncPolicy.DOMAIN)
                .addInstances(a, b);

        double response = a.doubleRequest.call(new DoubleRequest(4.0));
        assertEquals(0.0, response);

        response = a.doubleRequest.call(new DoubleRequest(8.0));
        assertEquals(0.0, response);
    }

    @Test
    public void deadlockResolutionAsync() {
        DeadlockA a = new DeadlockA();
        DeadlockB b = new DeadlockB();

        Ports.connect(a).and(b);

        Ports.domain("a", DispatchPolicy.ASYNCHRONOUS, SyncPolicy.COMPONENT)
                .addInstances(a);

        Ports.domain("b", DispatchPolicy.ASYNCHRONOUS, SyncPolicy.COMPONENT)
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

        Ports.domain("a", DispatchPolicy.PARALLEL, SyncPolicy.COMPONENT)
                .addInstances(a);

        Ports.domain("b", DispatchPolicy.PARALLEL, SyncPolicy.COMPONENT)
                .addInstances(b);

        double response = a.doubleRequest.call(new DoubleRequest(4.0));
        assertEquals(0.0, response);

        response = a.doubleRequest.call(new DoubleRequest(32.0));
        assertEquals(0.0, response);
    }

    @Test
    public void noDeadlock() {
        A a = new A();
        B b = new B();

        Ports.connect(a).and(b);

        Domain domainA = Ports.domain("a", DispatchPolicy.PARALLEL, SyncPolicy.COMPONENT)
                .addInstances(a);

        Fork<Double> fork = b.doubleRequest.fork(1000, DoubleRequest::new);

        PortsFuture<Double> future1 = b.slowRequest.callF(new SlowRequest(10.0));
        PortsFuture<Double> future2 = b.slowRequest.callF(new SlowRequest(1.0));

        double response2 = future2.get();
        double response1 = future1.get();

        fork.getNowEither()
                .forEach(either -> either.on(
                        value -> {
                        },
                        nothing -> fail("result expected"),
                        Assertions::fail));

        assertEquals(15.0, response1);
        assertEquals(1.5, response2);

        assertEquals(Runtime.getRuntime().availableProcessors(), domainA.getNumberOfThreadsCreated());
    }

    @Test
    public void portsEventException() {
        A a = new A();
        H h = new H();

        Ports.connect(a).and(h);

        assertDoesNotThrow(() -> a.intEvent.trigger(new IntEvent(1701)));

        assertEquals(PortsEventException.class, h.result.getClass());
        assertEquals("PortsEventException{org.timux.ports.MySpecialTestException: 1701}", h.result.toString());
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void errorLoopPrevention() {
        A a = new A();
        J j = new J();

        Ports.connect(a).and(j);

        assertDoesNotThrow(() -> a.intEvent.trigger(new IntEvent(1703)));

        assertEquals(PortsEventException.class, j.result.getClass());
        assertEquals("PortsEventException{org.timux.ports.MySpecialTestException: 1703}", j.result.toString());
    }
}
