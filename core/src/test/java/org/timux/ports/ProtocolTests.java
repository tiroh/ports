package org.timux.ports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.timux.ports.types.Container;
import org.timux.ports.types.Either;
import org.timux.ports.types.Either3;

import static org.junit.jupiter.api.Assertions.*;

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
    public void protocolsEither3Test() {
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
    public void protocolEventException() {
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
    public void protocolRequestException() {
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

        assertThrows(ExecutionException.class, () -> {
            Ports.protocol()
                    .with(EitherRequest.class, Double.class, String.class)
                    .call(new EitherRequest(1.0));
        });

        assertTrue(exceptionTriggered.value);
    }
}
