package org.timux.ports;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.timux.ports.types.Container;
import org.timux.ports.types.Either;
import org.timux.ports.types.Either3;
import org.timux.ports.types.Failure;
import org.timux.ports.types.Nothing;
import org.timux.ports.types.Success;
import org.timux.ports.types.Unknown;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class EitherTests {

    @Test
    public void eitherMap() {
        Either<Integer, Double> e = Either.b(1.5);

        Either<String, Double> x = e.mapA(i -> "Integer toString " + i);
        Either<Integer, String> y = e.mapB(d -> "Double toString " + d);

        String resultX = x.map(string -> "wrong", dbl -> "correct " + dbl);
        String resultY = y.map(integer -> "wrong", string -> string);

        assertEquals("correct 1.5", resultX);
        assertEquals("Double toString 1.5", resultY);
    }

    @Test
    public void either3Map() {
        Either3<Integer, Float, Double> e = Either3.b(1.5f);

        Either3<String, Float, Double> x = e.mapA(i -> "Integer toString " + i);
        Either3<Integer, String, Double> y = e.mapB(f -> "Float toString " + f);
        Either3<Integer, Float, String> z = e.mapC(d -> "Double toString " + d);

        String resultX = x.map(string -> "wrong string", flt -> "correct " + flt, dbl -> "wrong double");
        String resultY = y.map(integer -> "wrong integer", string -> string, dbl -> "wrong double");
        String resultZ = z.map(integer -> "wrong integer", flt -> "correct " + flt, string -> "wrong string");

        assertEquals("correct 1.5", resultX);
        assertEquals("Float toString 1.5", resultY);
        assertEquals("correct 1.5", resultZ);
    }

    @Test
    public void eitherAndThenOrElse() {
        Either<Integer, String> either1 = Either.a(1);

        either1.orElse(string -> fail("no string expected"))
                .andThenE(integer -> true)
                .on(
                        Assertions::assertTrue,
                        string -> fail("no string expected")
                );

        Either<Integer, String> either2 = Either.b("1.5");

        either2.andThenE(integer -> fail("no integer expected"))
                .orElse(Double::parseDouble)
                .on(
                        integer -> fail("no integer expected"),
                        value -> assertEquals(1.5, value)
                );
    }

    @Test
    public void either3AndThenOrElse() {
        Either3<Integer, Boolean, String> either1 = Either3.a(1);

        either1.orElse(string -> fail("no string expected"))
                .andThenE(integer -> true)
                .on(
                        Assertions::assertTrue,
                        bool -> fail("no boolean expected"),
                        string -> fail("no string expected")
                );

        Either3<Integer, Boolean, String> either2 = Either3.c("1.5");

        either2.andThenE(integer -> fail("no integer expected"))
                .orElse(Double::parseDouble)
                .on(
                        integer -> fail("no integer expected"),
                        bool -> fail("no boolean expected"),
                        value -> assertEquals(1.5, value)
                );
    }

    @Test
    public void eitherAndThenChainWithSuccess() {
        Container<Integer> imInteger = Container.of(null);
        Container<Double> imDouble = Container.of(null);

        Either<Double, Failure> result = eitherAndThenChain_test_function_1(1)
                .andThen(integer -> eitherAndThenChain_test_function_1(integer + 1))
                .andThen(integer -> eitherAndThenChain_test_function_1(integer + 1))
                .andThenDo(integer -> imInteger.value = integer)
                .andThen(integer -> eitherAndThenChain_test_function_2(integer + 1))
                .andThenDo(dbl -> imDouble.value = dbl)
                .orElseDo(failure -> fail("no orElseDo call expected"));

        assertEquals(6, imInteger.value);
        assertEquals(14.0, imDouble.value);

        result.on(
                dbl -> assertEquals(14.0, dbl),
                failure -> fail("no failure expected")
        );
    }

    @Test
    public void eitherAndThenChainWithFailure() {
        Container<Boolean> orElseCalled = Container.of(Boolean.FALSE);
        Container<Boolean> andThenCalled = Container.of(Boolean.FALSE);
        Container<Integer> finalValue = Container.of(null);

        Either<Integer, Failure> result = eitherAndThenChain_test_function_1(1)
                .andThenDo(integer -> finalValue.value = integer)
                .andThen(this::eitherAndThenChain_test_function_3)
                .orElseDo(failure -> orElseCalled.value = Boolean.TRUE)
                .andThenDo(integer -> andThenCalled.value = Boolean.TRUE);

        assertEquals(2, finalValue.value);
        assertTrue(orElseCalled.value);
        assertFalse(andThenCalled.value);
    }

    private Either<Integer, Failure> eitherAndThenChain_test_function_1(Integer x) {
        return Either.a(x + 1);
    }

    private Either<Double, Failure> eitherAndThenChain_test_function_2(Integer x) {
        return Either.a(x * 2.0);
    }

    private Either<Integer, Failure> eitherAndThenChain_test_function_3(Integer x) {
        return Either.b(Failure.of("test failure"));
    }

    @Test
    public void either3AndThenChainWithSuccess() {
        Container<Integer> imInteger = Container.of(null);
        Container<Double> imDouble = Container.of(null);

        Either3<Double, String, Failure> result = either3AndThenChain_test_function_1(1)
                .andThen(integer -> either3AndThenChain_test_function_1(integer + 1))
                .andThen(integer -> either3AndThenChain_test_function_1(integer + 1))
                .andThenDo(integer -> imInteger.value = integer)
                .andThen(integer -> either3AndThenChain_test_function_2(integer + 1))
                .andThenDo(dbl -> imDouble.value = dbl)
                .orElseDo(failure -> fail("no orElseDo call expected"));

        assertEquals(6, imInteger.value);
        assertEquals(14.0, imDouble.value);

        result.on(
                dbl -> assertEquals(14.0, dbl),
                string -> fail("no string expected"),
                failure -> fail("no failure expected")
        );
    }

    @Test
    public void either3AndThenChainWithFailure() {
        Container<Boolean> orElseCalled = Container.of(Boolean.FALSE);
        Container<Boolean> andThenCalled = Container.of(Boolean.FALSE);
        Container<Integer> finalValue = Container.of(null);

        Either3<Integer, String, Failure> result = either3AndThenChain_test_function_1(1)
                .andThenDo(integer -> finalValue.value = integer)
                .andThen(this::either3AndThenChain_test_function_3)
                .orElseDo(failure -> orElseCalled.value = Boolean.TRUE)
                .andThenDo(integer -> andThenCalled.value = Boolean.TRUE);

        assertEquals(2, finalValue.value);
        assertTrue(orElseCalled.value);
        assertFalse(andThenCalled.value);
    }

    private Either3<Integer, String, Failure> either3AndThenChain_test_function_1(Integer x) {
        return Either3.a(x + 1);
    }

    private Either3<Double, String, Failure> either3AndThenChain_test_function_2(Integer x) {
        return Either3.a(x * 2.0);
    }

    private Either3<Integer, String, Failure> either3AndThenChain_test_function_3(Integer x) {
        return Either3.c(Failure.of("test failure"));
    }

    @Test
    public void eitherXFailureResponse() {
        EitherA a = new EitherA();
        EitherB b = new EitherB();

        Ports.connect(a).and(b);

        Either<Integer, Failure> response = a.eitherXFailureRequest.call(new EitherXFailureRequest("xfailure"));

        response.on(
                integer -> fail("no integer expected"),
                failure -> {
                    Throwable throwable = failure.getThrowable().get().getCause().getCause().getCause();
                    assertEquals(MySpecialTestException.class, throwable.getClass());
                    assertEquals("xfailure", throwable.getMessage());
                }
        );
    }

    @Test
    public void eitherXYResponse() {
        EitherA a = new EitherA();
        EitherB b = new EitherB();

        Ports.connect(a).and(b);

        Exception exception = assertThrows(
                ExecutionException.class,
                () -> a.eitherXYRequest.call(new EitherXYRequest("xy"))
        );

        Throwable throwable = exception.getCause().getCause().getCause();

        assertEquals(MySpecialTestException.class, throwable.getClass());
        assertEquals("xy", throwable.getMessage());
    }

    @Test
    public void either3XFailureResponse() {
        EitherA a = new EitherA();
        EitherB b = new EitherB();

        Ports.connect(a).and(b);

        Either3<Integer, Nothing, Failure> response = a.either3XYFailureRequest.call(new Either3XYFailureRequest("xyfailure"));

        response.on(
                integer -> fail("no integer expected"),
                nothing -> fail("no nothing expected"),
                failure -> {
                    Throwable throwable = failure.getThrowable().get().getCause().getCause().getCause();
                    assertEquals(MySpecialTestException.class, throwable.getClass());
                    assertEquals("xyfailure", throwable.getMessage());
                }
        );
    }

    @Test
    public void either3XYResponse() {
        EitherA a = new EitherA();
        EitherB b = new EitherB();

        Ports.connect(a).and(b);

        Exception exception = assertThrows(
                ExecutionException.class,
                () -> a.either3XYZRequest.call(new Either3XYZRequest("xyz"))
        );

        Throwable throwable = exception.getCause().getCause().getCause();

        assertEquals(MySpecialTestException.class, throwable.getClass());
        assertEquals("xyz", throwable.getMessage());
    }

    @Test
    public void eitherOrElseOnce() {
        EitherA a = new EitherA();
        EitherB b = new EitherB();

        Ports.connect(a).and(b);

        Container<Failure> failure1 = Container.of(null);
        Container<Failure> failure2 = Container.of(null);

        a.eitherXFailureRequest.call(new EitherXFailureRequest("xfailure"))
                .orElseDo(f -> failure1.value = f)
                .orElseDoOnce(f -> fail("no orElse call expected (1)"))
                .andThenE(r -> a.either3XYFailureRequest.call(new Either3XYFailureRequest("xyfailure")))
                .orElseDo(f -> failure2.value = f)
                .orElseDoOnce(f -> fail("no orElse call expected (2)"))
                .andThenDo(r -> fail("no andThen call expected"));

        assertEquals("xfailure", failure1.value.getThrowable().get().getCause().getCause().getCause().getMessage());
        assertEquals(failure1.value, failure2.value);
    }

    @Test
    public void either3OrElseOnce() {
        EitherA a = new EitherA();
        EitherB b = new EitherB();

        Ports.connect(a).and(b);

        Container<Failure> failure1 = Container.of(null);
        Container<Failure> failure2 = Container.of(null);

        a.either3XYFailureRequest.call(new Either3XYFailureRequest("xyfailure"))
                .orElseDo(f -> failure1.value = f)
                .orElseDoOnce(f -> fail("no orElse call expected (1)"))
                .andThenE(r -> a.eitherXFailureRequest.call(new EitherXFailureRequest("xfailure")))
                .orElseDo(f -> failure2.value = f)
                .orElseDoOnce(f -> fail("no orElse call expected (2)"))
                .andThenDo(r -> fail("no andThen call expected"));

        assertEquals("xyfailure", failure1.value.getThrowable().get().getCause().getCause().getCause().getMessage());
        assertEquals(failure1.value, failure2.value);
    }

    @Test
    public void eitherConstructorsAndIsTypeMethods() {
        Either<Success, Failure> successEither = Either.success();

        Either<Success, Failure> failureEither = Either.failure();
        Either<Success, Failure> failureEitherMsg = Either.failure("message1");
        Either<Success, Failure> failureEitherT = Either.failure(new RuntimeException());
        Either<Success, Failure> failureEitherMsgT = Either.failure("message2", new RuntimeException());

        Either<Success, Nothing> nothingEither = Either.nothing();
        Either<Success, Unknown> unknownEither = Either.unknown();

        assertTrue(successEither.isSuccess());
        assertFalse(successEither.isFailure());
        assertFalse(successEither.isUnknown());
        assertFalse(successEither.isNothing());
        assertTrue(successEither.getA().isPresent());
        assertFalse(successEither.getB().isPresent());

        assertFalse(failureEither.isSuccess());
        assertTrue(failureEither.isFailure());
        assertFalse(failureEither.isUnknown());
        assertFalse(failureEither.isNothing());
        assertFalse(failureEither.getA().isPresent());
        assertTrue(failureEither.getB().isPresent());

        assertFalse(failureEitherMsg.isSuccess());
        assertTrue(failureEitherMsg.isFailure());
        assertFalse(failureEitherMsg.isUnknown());
        assertFalse(failureEitherMsg.isNothing());
        assertFalse(failureEitherMsg.getA().isPresent());
        assertEquals("message1", failureEitherMsg.getB().get().getMessage());

        assertFalse(failureEitherT.isSuccess());
        assertTrue(failureEitherT.isFailure());
        assertFalse(failureEitherT.isUnknown());
        assertFalse(failureEitherT.isNothing());
        assertFalse(failureEitherT.getA().isPresent());
        assertSame(failureEitherT.getB().get().getThrowable().get().getClass(), RuntimeException.class);

        assertFalse(failureEitherMsgT.isSuccess());
        assertTrue(failureEitherMsgT.isFailure());
        assertFalse(failureEitherMsgT.isUnknown());
        assertFalse(failureEitherMsgT.isNothing());
        assertFalse(failureEitherMsgT.getA().isPresent());
        assertEquals("message2", failureEitherMsgT.getB().get().getMessage());
        assertSame(failureEitherMsgT.getB().get().getThrowable().get().getClass(), RuntimeException.class);

        assertFalse(nothingEither.isSuccess());
        assertFalse(nothingEither.isFailure());
        assertTrue(nothingEither.isNothing());
        assertFalse(nothingEither.isUnknown());
        assertFalse(nothingEither.getA().isPresent());
        assertTrue(nothingEither.getB().isPresent());

        assertFalse(unknownEither.isSuccess());
        assertFalse(unknownEither.isFailure());
        assertFalse(unknownEither.isNothing());
        assertTrue(unknownEither.isUnknown());
        assertFalse(unknownEither.getA().isPresent());
        assertTrue(unknownEither.getB().isPresent());
    }

    @Test
    public void either3ConstructorsAndIsTypeMethods() {
        Either3<Success, Nothing, Failure> successEither = Either3.success();

        Either3<Success, Nothing, Failure> failureEither = Either3.failure();
        Either3<Success, Unknown, Failure> failureEitherMsg = Either3.failure("message1");
        Either3<Success, String, Failure> failureEitherT = Either3.failure(new RuntimeException());
        Either3<Success, Integer, Failure> failureEitherMsgT = Either3.failure("message2", new RuntimeException());

        Either3<Success, Nothing, Failure> nothingEither = Either3.nothing();
        Either3<Success, Unknown, Failure> unknownEither = Either3.unknown();

        assertTrue(successEither.isSuccess());
        assertFalse(successEither.isFailure());
        assertFalse(successEither.isUnknown());
        assertFalse(successEither.isNothing());
        assertTrue(successEither.getA().isPresent());
        assertFalse(successEither.getB().isPresent());
        assertFalse(successEither.getC().isPresent());

        assertFalse(failureEither.isSuccess());
        assertTrue(failureEither.isFailure());
        assertFalse(failureEither.isUnknown());
        assertFalse(failureEither.isNothing());
        assertFalse(failureEither.getA().isPresent());
        assertFalse(failureEither.getB().isPresent());
        assertTrue(failureEither.getC().isPresent());

        assertFalse(failureEitherMsg.isSuccess());
        assertTrue(failureEitherMsg.isFailure());
        assertFalse(failureEitherMsg.isUnknown());
        assertFalse(failureEitherMsg.isNothing());
        assertFalse(failureEitherMsg.getA().isPresent());
        assertFalse(failureEitherMsg.getB().isPresent());
        assertEquals("message1", failureEitherMsg.getC().get().getMessage());

        assertFalse(failureEitherT.isSuccess());
        assertTrue(failureEitherT.isFailure());
        assertFalse(failureEitherT.isUnknown());
        assertFalse(failureEitherT.isNothing());
        assertFalse(failureEitherT.getA().isPresent());
        assertFalse(failureEitherT.getB().isPresent());
        assertSame(failureEitherT.getC().get().getThrowable().get().getClass(), RuntimeException.class);

        assertFalse(failureEitherMsgT.isSuccess());
        assertTrue(failureEitherMsgT.isFailure());
        assertFalse(failureEitherMsgT.isUnknown());
        assertFalse(failureEitherMsgT.isNothing());
        assertFalse(failureEitherMsgT.getA().isPresent());
        assertFalse(failureEitherMsgT.getB().isPresent());
        assertEquals("message2", failureEitherMsgT.getC().get().getMessage());
        assertSame(failureEitherMsgT.getC().get().getThrowable().get().getClass(), RuntimeException.class);

        assertFalse(nothingEither.isSuccess());
        assertFalse(nothingEither.isFailure());
        assertTrue(nothingEither.isNothing());
        assertFalse(nothingEither.isUnknown());
        assertFalse(nothingEither.getA().isPresent());
        assertTrue(nothingEither.getB().isPresent());
        assertFalse(nothingEither.getC().isPresent());

        assertFalse(unknownEither.isSuccess());
        assertFalse(unknownEither.isFailure());
        assertFalse(unknownEither.isNothing());
        assertTrue(unknownEither.isUnknown());
        assertFalse(unknownEither.getA().isPresent());
        assertTrue(unknownEither.getB().isPresent());
        assertFalse(unknownEither.getC().isPresent());
    }

    @Test
    public void eitherSuccessOrFailureMethods() {
        Container<Boolean> runnableStatus = Container.of(Boolean.FALSE);

        Supplier<Integer> okSupplier = () -> 1;
        Runnable okRunnable = () -> runnableStatus.value = Boolean.TRUE;

        Supplier<Integer> nullSupplier = () -> null;

        Supplier<Integer> failureSupplier = () -> { throw new RuntimeException("A"); };
        Runnable failureRunnable = () -> { throw new RuntimeException("B"); };

        Either<Integer, Failure> okSupplierEither = Either.valueOrFailure(okSupplier);
        Either<Success, Failure> okSupplierSFEither = Either.successOrFailure(okSupplier);
        Either<Success, Failure> okRunnableEither = Either.successOrFailure(okRunnable);

        Either<Integer, Failure> failureSupplierEither = Either.valueOrFailure(failureSupplier);
        Either<Success, Failure> failureSupplierSFEither = Either.successOrFailure(failureSupplier);
        Either<Success, Failure> failureRunnableEither = Either.successOrFailure(failureRunnable);

        Either<Success, Failure> okSFEither = Either.successOrFailure(okSupplierEither);
        Either<Success, Failure> failureSFEither = Either.successOrFailure(failureSupplierEither);

        okSupplierEither.on(
                integer -> assertEquals(1, integer),
                failure -> fail("no failure expected")
        );

        okSupplierSFEither.on(
                success -> {},
                failure -> fail("no failure expected")
        );

        okRunnableEither.on(
                success -> assertTrue(runnableStatus.value),
                failure -> fail("no failure expected")
        );

        failureSupplierEither.on(
                integer -> fail("no integer expected"),
                failure -> assertEquals("A", failure.getThrowable().get().getMessage())
        );

        failureSupplierSFEither.on(
                success -> fail("no success expected"),
                failure -> assertEquals("A", failure.getThrowable().get().getMessage())
        );

        failureRunnableEither.on(
                success -> fail("no success expected"),
                failure -> assertEquals("B", failure.getThrowable().get().getMessage())
        );

        okSFEither.on(
                success -> {},
                failure -> fail("no failure expected")
        );

        failureSFEither.on(
                success -> fail("no success expected"),
                failure -> assertEquals("A", failure.getThrowable().get().getMessage())
        );
    }

    @Test
    public void either3SuccessOrFailureMethods() {
        Supplier<Integer> okSupplier = () -> 1;
        Supplier<Integer> nullSupplier = () -> null;
        Supplier<Integer> failureSupplier = () -> { throw new RuntimeException("A"); };

        Either3<Integer, Nothing, Failure> either3Integer = Either3.valueOrNothingOrFailure(okSupplier);
        Either3<Integer, Nothing, Failure> either3Nothing = Either3.valueOrNothingOrFailure(nullSupplier);
        Either3<Integer, Nothing, Failure> either3Failure = Either3.valueOrNothingOrFailure(failureSupplier);

        Either<Success, Failure> eitherIntegerSuccess = Either.successOrFailure(either3Integer);
        Either<Success, Failure> eitherNothingFailure = Either.successOrFailure(either3Nothing);
        Either<Success, Failure> eitherFailureFailure = Either.successOrFailure(either3Failure);

        either3Integer.on(
                integer -> assertEquals(1, integer),
                nothing -> fail("no nothing expected"),
                failure -> fail("no failure expected")
        );

        either3Nothing.on(
                integer -> fail("no integer expected"),
                nothing -> {},
                failure -> fail("no failure expected")
        );

        either3Failure.on(
                integer -> fail("no integer expected"),
                nothing -> fail("no nothing expected"),
                failure -> assertEquals("A", failure.getThrowable().get().getMessage())
        );

        eitherIntegerSuccess.on(
                success -> {},
                failure -> fail("no failure expected")
        );

        eitherNothingFailure.on(
                success -> {},
                failure -> fail("no failure expected")
        );

        eitherFailureFailure.on(
                success -> fail("no success expected"),
                failure -> assertEquals("A", failure.getThrowable().get().getMessage())
        );
    }
}
