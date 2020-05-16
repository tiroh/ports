package org.timux.ports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.timux.ports.testapp.component.IntEvent;
import org.timux.ports.types.Either;
import org.timux.ports.types.Either3;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProtocolTests {

    @BeforeEach
    public void setupEach() {
        Ports.releaseProtocols();
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
}
