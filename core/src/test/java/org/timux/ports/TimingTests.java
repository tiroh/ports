package org.timux.ports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.timux.ports.types.Either;
import org.timux.ports.types.Either3;
import org.timux.ports.types.Nothing;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class TimingTests {

    @BeforeAll
    public static void beforeAll() {
        Executor.TEST_API_MAX_NUMBER_OF_THREADS = 10;
        Executor.TEST_API_IDLE_LIFETIME_MS = 2000;
    }

    @AfterEach
    public void afterEach() {
        Ports.reset();
    }

    @Test
    public void forkGet() {
        A a = new A();
        B b = new B();

        Ports.connect(a).and(b);

        Ports.domain("test-a", DispatchPolicy.PARALLEL, SyncPolicy.NONE)
                .addClasses(A.class);

        List<Either<Double, Throwable>> results = assertTimeout(Duration.ofMillis(1100), () -> {
            Fork<Double> fork = b.slowRequest.fork(10, SlowRequest::new);
            return fork.getEither();
        });

        assertEquals(10, results.size());

        for (int i = 0; i < results.size(); i++) {
            int finalI = i;

            results.get(i).on(
                    value -> assertEquals(finalI * 1.5, value),
                    throwable -> fail("index " + finalI + ": request should not fail: ", throwable)
            );
        }
    }

    @Test
    public void forkGetNow() {
        A a = new A();
        B b = new B();

        Ports.domain("test-a", DispatchPolicy.PARALLEL, SyncPolicy.NONE)
                .addInstances(a);

        Ports.domain("test-b", DispatchPolicy.PARALLEL, SyncPolicy.NONE)
                .addInstances(b);

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
        A a = new A();
        B b = new B();

        Ports.domain("test-a", DispatchPolicy.PARALLEL, SyncPolicy.NONE)
                .addPackages("org.timux");

        Ports.domain("test-b", DispatchPolicy.PARALLEL, SyncPolicy.NONE)
                .addInstances(b);

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
}
