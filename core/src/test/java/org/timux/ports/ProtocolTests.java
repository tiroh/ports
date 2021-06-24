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
import org.timux.ports.types.Either3;
import org.timux.ports.types.Failure;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ProtocolTests {

    @AfterEach
    public void afterEach() {
        Ports.releaseProtocols();
    }

    @Test
    public void protocolsSmokeTest() {
        A a = new A();
        B b = new B();

        Ports.connect(a).and(b);

        Ports.register(a, b);

        Container<Boolean> firstActionA = Container.of(false);
        Container<Boolean> secondActionA = Container.of(false);
        Container<Boolean> firstActionB = Container.of(false);
        Container<Boolean> secondActionB = Container.of(false);
        Container<Boolean> firstActionC = Container.of(false);
        Container<Boolean> firstActionD = Container.of(false);

        Container<Integer> doubleRequestIndex = Container.of(0);
        double[] doubleRequestValues = {50.0, 2.5, 2.0, 2.5, 4.0};

        Container<Boolean> doubleRequestResponse = Container.of(false);

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
                        System.out.println(x.getData());
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
    public void protocolsEitherWithWhenTest() {
        D d = new D();

        Ports.register(d);

        Container<Either<Double, String>> eitherValue = Container.of(null);

        Ports.protocol()
            .when(EitherRequest.class, Double.class, String.class)
                .requests()
                .respond(request -> Either.a(request.getValue()))
            .when(EitherRequest.class, Double.class, String.class)
                .responds()
                .storeIn(eitherValue);

        Ports.protocol()
            .with(EitherRequest.class, Double.class, String.class)
                .call(new EitherRequest(1.0));

        assertNotNull(eitherValue.value);
        assertEquals(1.0, eitherValue.value.map(x -> x, Double::parseDouble), 0.0);
    }

    @Test
    public void protocolsEither3WithWhenTest() {
        D d = new D();

        Ports.register(d);

        Container<Either3<Double, Integer, String>> eitherValue = Container.of(null);

        Ports.protocol()
            .when(Either3Request.class, Double.class, Integer.class, String.class)
                .requests()
                .respond(request -> Either3.b(request.getValue()))
            .when(Either3Request.class, Double.class, Integer.class, String.class)
                .responds()
                .storeIn(eitherValue);

        Ports.protocol()
            .with(Either3Request.class, Double.class, Integer.class, String.class)
                .call(new Either3Request(1));

        assertNotNull(eitherValue.value);
        assertEquals(1.0, eitherValue.value.map(x -> x, x -> (double) x, Double::parseDouble), 0.0);
    }

    @Test
    public void protocolsEitherAndEither3WithTest() {
        E e = new E();

        Ports.register(e);

        Container<Either<Double, String>> eitherValue = Container.of(null);
        Container<Either3<Double, Integer, String>> either3Value = Container.of(null);

        Ports.protocol()
            .when(EitherRequest.class, Double.class, String.class)
                .responds()
                .storeIn(eitherValue)
            .when(Either3Request.class, Double.class, Integer.class, String.class)
                .responds()
                .storeIn(either3Value);
        ;

        Ports.protocol()
            .with(EitherRequest.class, Double.class, String.class)
                .call(new EitherRequest(1.0))
            .with(Either3Request.class, Double.class, Integer.class, String.class)
                .call(new Either3Request(1));

        assertNotNull(eitherValue.value);
        assertNotNull(either3Value.value);

        assertEquals(2.0, eitherValue.value.map(x -> x, Double::parseDouble), 0.0);
        assertEquals(2.5, either3Value.value.map(x -> x, x -> (double) x, Double::parseDouble), 0.0);
    }

    @Test
    public void protocolsEventException() {
        A a = new A();

        Ports.register(a);

        Container<Boolean> exceptionTriggered = Container.of(Boolean.FALSE);

        Ports.protocol()
            .when(IntEvent.class)
                .triggers()
                .do_(() -> {
                    exceptionTriggered.value = Boolean.TRUE;
                    throw new MySpecialTestException("this is expected");
                });

        assertDoesNotThrow(() -> {
            Ports.protocol()
                .with(IntEvent.class)
                    .trigger(new IntEvent(1));
        });

        assertTrue(exceptionTriggered.value);
    }

    @Test
    public void protocolsRequestException() {
        D d = new D();

        Ports.register(d);

        Container<Boolean> exceptionTriggered = Container.of(Boolean.FALSE);

        Ports.protocol()
            .when(EitherRequest.class, Double.class, String.class)
                .requests()
                .respond(request -> {
                    exceptionTriggered.value = Boolean.TRUE;
                    throw new MySpecialTestException("?");
                });

        assertThrows(PortsExecutionException.class, () -> {
            Ports.protocol()
                .with(EitherRequest.class, Double.class, String.class)
                    .call(new EitherRequest(1.0));
        });

        assertTrue(exceptionTriggered.value);
    }

    @Test
    public void protocolsFailureCaptureWithReceiverComponent() {
        F f = new F();

        Ports.register(f);

        Container<Either<Integer, Failure>> result = Container.of(null);

        Ports.protocol()
            .when(EitherXFailureRequest.class, Integer.class, Failure.class)
                .responds()
                .storeIn(result);

        Ports.protocol()
            .with(EitherXFailureRequest.class, Integer.class, Failure.class)
                .call(new EitherXFailureRequest("this is supposed to fail"));

        assertTrue(result.value.isFailure());
    }

    @Test
    public void protocolsFailureCaptureWithReceiverComponentAndFaultInjection() {
        F f = new F();

        Ports.register(f);

        Container<Either<Integer, Failure>> result = Container.of(null);

        Ports.protocol()
            .when(EitherXFailureRequest.class, Integer.class, Failure.class)
                .requests()
                .respond(request -> {
                    throw new MySpecialTestException(request.getMessage());
                })
            .when(EitherXFailureRequest.class, Integer.class, Failure.class)
                .responds()
                .storeIn(result);

        Ports.protocol()
            .with(EitherXFailureRequest.class, Integer.class, Failure.class)
                .call(new EitherXFailureRequest("this is supposed to fail"));

        assertTrue(result.value.isFailure());
    }

    @Test
    public void protocolsFailureCaptureWithFaultInjectionAndGetFirstNonPortsThrowable() {
        F f = new F();

        Ports.register(f);

        Container<Either<Integer, Failure>> result = Container.of(null);

        Ports.protocol()
                .when(EitherXFailureRequest.class, Integer.class, Failure.class)
                .requests()
                .respond(request -> {
                    throw new PortsExecutionException(new MySpecialTestException(request.getMessage()));
                })
                .when(EitherXFailureRequest.class, Integer.class, Failure.class)
                .responds()
                .storeIn(result);

        Ports.protocol()
                .with(EitherXFailureRequest.class, Integer.class, Failure.class)
                .call(new EitherXFailureRequest("message"));

        result.value.on(
                integer -> fail("no integer expected"),
                failure -> {
                    assertEquals(PortsExecutionException.class, failure.getThrowable().map(Throwable::getClass).orElse(null));
                    assertEquals(MySpecialTestException.class, failure.getThrowable().map(Throwable::getCause).map(Throwable::getClass).orElse(null));
                    assertEquals(MySpecialTestException.class, failure.getFirstNonPortsThrowable().map(Throwable::getClass).orElse(null));
                    assertEquals("message", failure.getFirstNonPortsThrowable().map(Throwable::getMessage).orElse(""));
                });
    }

    @Test
    public void protocolsFailureCaptureWithoutReceiverComponentAndWithFaultInjection() {
        Container<Either<Integer, Failure>> result = Container.of(null);

        Ports.protocol()
            .when(EitherXFailureRequest.class, Integer.class, Failure.class)
                .requests()
                .respond(request -> {
                    throw new MySpecialTestException(request.getMessage());
                })
            .when(EitherXFailureRequest.class, Integer.class, Failure.class)
                .responds()
                .storeIn(result);

        Ports.protocol()
            .with(EitherXFailureRequest.class, Integer.class, Failure.class)
                .call(new EitherXFailureRequest("this is supposed to fail"));

        assertTrue(result.value.isFailure());
    }

    @Test
    public void protocolsFailureCaptureWithOwnerComponentAndWithFaultInjection() {
        G g = new G();

        Ports.register(g);

        Container<Either<Integer, Failure>> result = Container.of(null);

        Ports.protocol()
            .when(EitherXFailureRequest.class, Integer.class, Failure.class)
                .requests()
                .respond(request -> {
                    throw new MySpecialTestException(request.getMessage());
                })
            .when(EitherXFailureRequest.class, Integer.class, Failure.class)
                .responds()
                .storeIn(result);

        Ports.protocol()
            .with(EitherXFailureRequest.class, Integer.class, Failure.class, g)
                .call(new EitherXFailureRequest("this is supposed to fail"));

        assertTrue(result.value.isFailure());
    }
}
