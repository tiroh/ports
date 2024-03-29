/*
 * Copyright 2018-2022 Tim Rohlfs
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
import org.junit.jupiter.api.Test;
import org.timux.ports.types.*;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class EitherTests {

    @AfterEach
    public void afterEach() {
        Ports.reset();
    }

    @Test
    public void eitherOf() {
        Either<Integer, Double> first1 = Either.of(1, null);
        Either<Integer, Double> second = Either.of(null, 1.5);
        Either<Integer, Double> first2 = Either.of(2, 2.5);

        Either<String, String> firstL1 = Either.of(Arrays.asList("a", null));
        Either<String, String> firstL2 = Either.of(Arrays.asList(null, "b"));
        Either<String, String> firstL3 = Either.of(Arrays.asList("a", "b"));

        Either<Integer, Double> firstP1 = Either.of(Tuple.of(1, null));
        Either<Integer, Double> secondP = Either.of(Tuple.of(null, 1.5));
        Either<Integer, Double> firstP2 = Either.of(Tuple.of(2, 2.5));

        assertEquals(1, first1.getAOrThrow());
        assertEquals(1.5, second.getBOrThrow());
        assertEquals(2, first2.getAOrThrow());

        assertEquals("a", firstL1.getAOrThrow());
        assertEquals("b", firstL2.getBOrThrow());
        assertEquals("a", firstL3.getAOrThrow());

        assertEquals(1, firstP1.getAOrThrow());
        assertEquals(1.5, secondP.getBOrThrow());
        assertEquals(2, firstP2.getAOrThrow());

        assertThrows(IllegalArgumentException.class, () -> Either.of(null, null));
        assertThrows(IllegalArgumentException.class, () -> Either.of(Arrays.asList(null, null)));
        assertThrows(IllegalArgumentException.class, () -> Either.of(Tuple.of(null, null)));
    }

    @Test
    public void either3Of() {
        Either3<Integer, Double, String> first1 = Either3.of(1, null, null);
        Either3<Integer, Double, String> second = Either3.of(null, 1.5, null);
        Either3<Integer, Double, String> third = Either3.of(null, null, "test");

        Either3<Integer, Double, String> first2 = Either3.of(null, 2.5, "test");
        Either3<Integer, Double, String> first3 = Either3.of(2, null, "test");
        Either3<Integer, Double, String> first4 = Either3.of(2, 2.5, "test");

        Either3<String, String, String> firstL1 = Either3.of(Arrays.asList("a", null, null));
        Either3<String, String, String> firstL2 = Either3.of(Arrays.asList(null, "b", null));
        Either3<String, String, String> firstL3 = Either3.of(Arrays.asList(null, null, "c"));

        Either3<String, String, String> firstL4 = Either3.of(Arrays.asList("a", "b", null));
        Either3<String, String, String> firstL5 = Either3.of(Arrays.asList(null, "b", "c"));
        Either3<String, String, String> firstL6 = Either3.of(Arrays.asList("a", null, "c"));

        Either3<Integer, Double, String> firstP1 = Either3.of(Tuple.of(1, null, null));
        Either3<Integer, Double, String> secondP = Either3.of(Tuple.of(null, 1.5, null));
        Either3<Integer, Double, String> thirdP = Either3.of(Tuple.of(null, null, "test"));

        Either3<Integer, Double, String> firstP2 = Either3.of(Tuple.of(null, 2.5, "test"));
        Either3<Integer, Double, String> firstP3 = Either3.of(Tuple.of(2, null, "test"));
        Either3<Integer, Double, String> firstP4 = Either3.of(Tuple.of(2, 2.5, "test"));

        assertEquals(1, first1.getAOrThrow());
        assertEquals(1.5, second.getBOrThrow());
        assertEquals("test", third.getCOrThrow());

        assertEquals(2.5, first2.getBOrThrow());
        assertEquals(2, first3.getAOrThrow());
        assertEquals(2, first4.getAOrThrow());

        assertEquals("a", firstL1.getAOrThrow());
        assertEquals("b", firstL2.getBOrThrow());
        assertEquals("c", firstL3.getCOrThrow());

        assertEquals("a", firstL4.getAOrThrow());
        assertEquals("b", firstL5.getBOrThrow());
        assertEquals("a", firstL6.getAOrThrow());

        assertEquals(1, firstP1.getAOrThrow());
        assertEquals(1.5, secondP.getBOrThrow());
        assertEquals("test", thirdP.getCOrThrow());

        assertEquals(2.5, firstP2.getBOrThrow());
        assertEquals(2, firstP3.getAOrThrow());
        assertEquals(2, firstP4.getAOrThrow());

        assertThrows(IllegalArgumentException.class, () -> Either3.of(null, null, null));
        assertThrows(IllegalArgumentException.class, () -> Either3.of(Arrays.asList(null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> Either3.of(Tuple.of(null, null, null)));
    }

    @Test
    public void nullable() {
        Either<Integer, Nothing> e = Either.ofNullable(null);
        Either3<Integer, Double, Nothing> e3 = Either3.ofNullables(null, null);

        Container<Boolean> eNC = Container.of(Boolean.FALSE);
        Container<Boolean> e3NC = Container.of(Boolean.FALSE);

        e.on(
                integer -> fail("no integer expected"),
                nothing -> eNC.value = Boolean.TRUE
        );

        e3.on(
                integer -> fail("no integer expected"),
                dbl -> fail("no double expected"),
                nothing -> e3NC.value = Boolean.TRUE
        );

        assertTrue(eNC.value);
        assertTrue(e3NC.value);
    }

    @Test
    public void eitherOn() {
        Either<Integer, Double> intEither = Either.a(1);
        Either<Integer, Double> doubleEither = Either.b(2.0);

        Container<Integer> intC = Container.of(null);
        Container<Double> doubleC = Container.of(null);

        intEither.on(
                integer -> intC.value = integer,
                dbl -> fail("no double expected")
        );

        doubleEither.on(
                integer -> fail("no integer expected"),
                dbl -> doubleC.value = dbl
        );

        assertEquals(1, intC.value);
        assertEquals(2.0, doubleC.value);
    }

    @Test
    public void either3On() {
        Either3<Integer, Float, Double> intEither3 = Either3.a(1);
        Either3<Integer, Float, Double> floatEither3 = Either3.b(2.0f);
        Either3<Integer, Float, Double> doubleEither3 = Either3.c(3.0);

        Container<Integer> intC = Container.of(null);
        Container<Double> doubleC = Container.of(null);
        Container<Float> floatC = Container.of(null);

        intEither3.on(
                integer -> intC.value = integer,
                flt -> fail("no float expected"),
                dbl -> fail("no double expected")
        );

        floatEither3.on(
                integer -> fail("no integer expected"),
                flt -> floatC.value = flt,
                dbl -> fail("no double expected")
        );

        doubleEither3.on(
                integer -> fail("no integer expected"),
                flt -> fail("no float expected"),
                dbl -> doubleC.value = dbl
        );

        intEither3.on(
                either -> either.on(
                        integer -> intC.value = integer,
                        flt -> fail("no float expected")
                ),
                dbl -> fail("no double expected")
        );

        floatEither3.on(
                either -> either.on(
                        integer -> fail("no integer expected"),
                        flt -> floatC.value = flt
                ),
                dbl -> fail("no double expected")
        );

        doubleEither3.on(
                either -> fail("no either expected"),
                dbl -> doubleC.value = dbl
        );

        assertEquals(1, intC.value);
        assertEquals(2.0f, floatC.value);
        assertEquals(3.0, doubleC.value);
    }

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

        String resultXE = x.map(either -> either.map(string -> "wrong string", flt -> "correct " + flt), dbl -> "wrong double");
        String resultYE = y.map(either -> either.map(integer -> "wrong integer", string -> string), dbl -> "wrong double");
        String resultZE = z.map(either -> either.map(integer -> "wrong integer", flt -> "correct " + flt), string -> "wrong string");

        assertEquals("correct 1.5", resultX);
        assertEquals("Float toString 1.5", resultY);
        assertEquals("correct 1.5", resultZ);

        assertEquals("correct 1.5", resultXE);
        assertEquals("Float toString 1.5", resultYE);
        assertEquals("correct 1.5", resultZE);
    }

    @Test
    public void eitherFinally() {
        Container<Integer> f1 = Container.of(0);
        Container<Integer> f2 = Container.of(0);

        Either<Integer, String> e1 = Either.a(1);
        Either3<Integer, Double, String> e2 = Either3.a(11);

        int r1 = e1.finallyDo(() -> f1.value = 1)
                .on(
                        integer -> assertEquals(1, integer),
                        string -> fail("no string expected"))
                .finallyMap(() -> 17);

        int r2 = e2.finallyDo(() -> f2.value = 2)
                .on(
                        integer -> assertEquals(11, integer),
                        dbl -> fail("no double expected"),
                        string -> fail("no string expected"))
                .finallyMap(() -> 18);

        assertEquals(1, f1.value);
        assertEquals(17, r1);

        assertEquals(2, f2.value);
        assertEquals(18, r2);
    }

    @Test
    public void eitherAndThenOrElse() {
        Either<Integer, String> either1 = Either.a(1);

        either1.orElseMap(string -> fail("no string expected"))
                .andThenMapE(integer -> true)
                .on(
                        Assertions::assertTrue,
                        string -> fail("no string expected")
                );

        Either<Integer, String> either2 = Either.b("1.5");

        either2.andThenMapE(integer -> fail("no integer expected"))
                .orElseMap(Double::parseDouble)
                .on(
                        integer -> fail("no integer expected"),
                        value -> assertEquals(1.5, value)
                );
    }

    @Test
    public void either3AndThenOrElse() {
        Either3<Integer, Boolean, String> either1 = Either3.a(1);

        either1.orElseMap(string -> fail("no string expected"))
                .andThenMapE(integer -> true)
                .on(
                        Assertions::assertTrue,
                        bool -> fail("no boolean expected"),
                        string -> fail("no string expected")
                );

        Either3<Integer, Boolean, String> either2 = Either3.c("1.5");

        either2.andThenMapE(integer -> fail("no integer expected"))
                .orElseMap(Double::parseDouble)
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
                .andThenMap(integer -> eitherAndThenChain_test_function_1(integer + 1))
                .andThenMap(integer -> eitherAndThenChain_test_function_1(integer + 1))
                .andThenDo(integer -> imInteger.value = integer)
                .andThenMap(integer -> eitherAndThenChain_test_function_2(integer + 1))
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
                .andThenMap(this::eitherAndThenChain_test_function_3)
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
                .andThenMap(integer -> either3AndThenChain_test_function_1(integer + 1))
                .andThenMap(integer -> either3AndThenChain_test_function_1(integer + 1))
                .andThenDo(integer -> imInteger.value = integer)
                .andThenMap(integer -> either3AndThenChain_test_function_2(integer + 1))
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
                .andThenMap(this::either3AndThenChain_test_function_3)
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
                    Throwable throwable = failure.getThrowable().get().getCause();
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
                PortsExecutionException.class,
                () -> a.eitherXYRequest.call(new EitherXYRequest("xy"))
        );

        Throwable throwable = exception.getCause();

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
                    Throwable throwable = failure.getThrowable().get().getCause();
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
                PortsExecutionException.class,
                () -> a.either3XYZRequest.call(new Either3XYZRequest("xyz"))
        );

        Throwable throwable = exception.getCause();

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
                .andThenMapE(r -> a.either3XYFailureRequest.call(new Either3XYFailureRequest("xyfailure")))
                .orElseDo(f -> failure2.value = f)
                .orElseDoOnce(f -> fail("no orElse call expected (2)"))
                .andThenDo(r -> fail("no andThen call expected"));

        assertEquals("xfailure", failure1.value.getThrowable().get().getCause().getMessage());
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
                .andThenMapE(r -> a.eitherXFailureRequest.call(new EitherXFailureRequest("xfailure")))
                .orElseDo(f -> failure2.value = f)
                .orElseDoOnce(f -> fail("no orElse call expected (2)"))
                .andThenDo(r -> fail("no andThen call expected"));

        assertEquals("xyfailure", failure1.value.getThrowable().get().getCause().getMessage());
        assertEquals(failure1.value, failure2.value);
    }

    @Test
    public void eitherConstructorsAndIsTypeMethods() {
        Either<Success, Failure> successEither = Either.success();

        Either<Success, Failure> failureEither = Either.failure();
        Either<Success, Failure> failureEitherMsg = Either.failure("message1");
        Either<Success, Failure> failureEitherT = Either.failure(new RuntimeException());
        Either<Success, Failure> failureEitherMsgT = Either.failure("message2", new RuntimeException());

        Either<Success, Empty> emptyEither = Either.empty();
        Either<Success, Nothing> nothingEither = Either.nothing();
        Either<Success, Unknown> unknownEither = Either.unknown();

        assertTrue(successEither.isSuccess());
        assertFalse(successEither.isFailure());
        assertFalse(successEither.isUnknown());
        assertFalse(successEither.isEmpty());
        assertFalse(successEither.isNothing());
        assertTrue(successEither.getA().isPresent());
        assertFalse(successEither.getB().isPresent());

        assertFalse(failureEither.isSuccess());
        assertTrue(failureEither.isFailure());
        assertFalse(failureEither.isUnknown());
        assertFalse(failureEither.isEmpty());
        assertFalse(failureEither.isNothing());
        assertFalse(failureEither.getA().isPresent());
        assertTrue(failureEither.getB().isPresent());

        assertFalse(failureEitherMsg.isSuccess());
        assertTrue(failureEitherMsg.isFailure());
        assertFalse(failureEitherMsg.isUnknown());
        assertFalse(failureEitherMsg.isEmpty());
        assertFalse(failureEitherMsg.isNothing());
        assertFalse(failureEitherMsg.getA().isPresent());
        assertEquals("message1", failureEitherMsg.getB().get().getMessage());

        assertFalse(failureEitherT.isSuccess());
        assertTrue(failureEitherT.isFailure());
        assertFalse(failureEitherT.isUnknown());
        assertFalse(failureEitherT.isEmpty());
        assertFalse(failureEitherT.isNothing());
        assertFalse(failureEitherT.getA().isPresent());
        assertSame(failureEitherT.getB().get().getThrowable().get().getClass(), RuntimeException.class);

        assertFalse(failureEitherMsgT.isSuccess());
        assertTrue(failureEitherMsgT.isFailure());
        assertFalse(failureEitherMsgT.isUnknown());
        assertFalse(failureEitherMsgT.isEmpty());
        assertFalse(failureEitherMsgT.isNothing());
        assertFalse(failureEitherMsgT.getA().isPresent());
        assertEquals("message2", failureEitherMsgT.getB().get().getMessage());
        assertSame(failureEitherMsgT.getB().get().getThrowable().get().getClass(), RuntimeException.class);

        assertFalse(emptyEither.isSuccess());
        assertFalse(emptyEither.isFailure());
        assertFalse(emptyEither.isUnknown());
        assertTrue(emptyEither.isEmpty());
        assertFalse(emptyEither.isNothing());
        assertFalse(emptyEither.getA().isPresent());
        assertTrue(emptyEither.getB().isPresent());

        assertFalse(nothingEither.isSuccess());
        assertFalse(nothingEither.isFailure());
        assertFalse(nothingEither.isUnknown());
        assertFalse(nothingEither.isEmpty());
        assertTrue(nothingEither.isNothing());
        assertFalse(nothingEither.getA().isPresent());
        assertTrue(nothingEither.getB().isPresent());

        assertFalse(unknownEither.isSuccess());
        assertFalse(unknownEither.isFailure());
        assertTrue(unknownEither.isUnknown());
        assertFalse(unknownEither.isEmpty());
        assertFalse(unknownEither.isNothing());
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

        Either3<Success, Empty, Failure> emptyEitherB = Either3.emptyB();
        Either3<Success, Nothing, Failure> nothingEitherB = Either3.nothingB();
        Either3<Success, Unknown, Failure> unknownEitherB = Either3.unknownB();

        Either3<Success, Integer, Empty> emptyEitherC = Either3.emptyC();
        Either3<Success, Integer, Nothing> nothingEitherC = Either3.nothingC();
        Either3<Success, Integer, Unknown> unknownEitherC = Either3.unknownC();

        assertTrue(successEither.isSuccess());
        assertFalse(successEither.isFailure());
        assertFalse(successEither.isUnknown());
        assertFalse(successEither.isEmpty());
        assertFalse(successEither.isNothing());
        assertTrue(successEither.getA().isPresent());
        assertFalse(successEither.getB().isPresent());
        assertFalse(successEither.getC().isPresent());

        assertFalse(failureEither.isSuccess());
        assertTrue(failureEither.isFailure());
        assertFalse(failureEither.isUnknown());
        assertFalse(failureEither.isEmpty());
        assertFalse(failureEither.isNothing());
        assertFalse(failureEither.getA().isPresent());
        assertFalse(failureEither.getB().isPresent());
        assertTrue(failureEither.getC().isPresent());

        assertFalse(failureEitherMsg.isSuccess());
        assertTrue(failureEitherMsg.isFailure());
        assertFalse(failureEitherMsg.isUnknown());
        assertFalse(failureEitherMsg.isEmpty());
        assertFalse(failureEitherMsg.isNothing());
        assertFalse(failureEitherMsg.getA().isPresent());
        assertFalse(failureEitherMsg.getB().isPresent());
        assertEquals("message1", failureEitherMsg.getC().get().getMessage());

        assertFalse(failureEitherT.isSuccess());
        assertTrue(failureEitherT.isFailure());
        assertFalse(failureEitherT.isUnknown());
        assertFalse(failureEitherT.isEmpty());
        assertFalse(failureEitherT.isNothing());
        assertFalse(failureEitherT.getA().isPresent());
        assertFalse(failureEitherT.getB().isPresent());
        assertSame(failureEitherT.getC().get().getThrowable().get().getClass(), RuntimeException.class);

        assertFalse(failureEitherMsgT.isSuccess());
        assertTrue(failureEitherMsgT.isFailure());
        assertFalse(failureEitherMsgT.isUnknown());
        assertFalse(failureEitherMsgT.isEmpty());
        assertFalse(failureEitherMsgT.isNothing());
        assertFalse(failureEitherMsgT.getA().isPresent());
        assertFalse(failureEitherMsgT.getB().isPresent());
        assertEquals("message2", failureEitherMsgT.getC().get().getMessage());
        assertSame(failureEitherMsgT.getC().get().getThrowable().get().getClass(), RuntimeException.class);

        assertFalse(emptyEitherB.isSuccess());
        assertFalse(emptyEitherB.isFailure());
        assertFalse(emptyEitherB.isUnknown());
        assertTrue(emptyEitherB.isEmpty());
        assertFalse(emptyEitherB.isNothing());
        assertFalse(emptyEitherB.getA().isPresent());
        assertTrue(emptyEitherB.getB().isPresent());
        assertFalse(emptyEitherB.getC().isPresent());

        assertFalse(nothingEitherB.isSuccess());
        assertFalse(nothingEitherB.isFailure());
        assertFalse(nothingEitherB.isUnknown());
        assertFalse(nothingEitherB.isEmpty());
        assertTrue(nothingEitherB.isNothing());
        assertFalse(nothingEitherB.getA().isPresent());
        assertTrue(nothingEitherB.getB().isPresent());
        assertFalse(nothingEitherB.getC().isPresent());

        assertFalse(unknownEitherB.isSuccess());
        assertFalse(unknownEitherB.isFailure());
        assertTrue(unknownEitherB.isUnknown());
        assertFalse(unknownEitherB.isNothing());
        assertFalse(unknownEitherB.isEmpty());
        assertFalse(unknownEitherB.getA().isPresent());
        assertTrue(unknownEitherB.getB().isPresent());
        assertFalse(unknownEitherB.getC().isPresent());

        assertFalse(emptyEitherC.isSuccess());
        assertFalse(emptyEitherC.isFailure());
        assertFalse(emptyEitherC.isUnknown());
        assertTrue(emptyEitherC.isEmpty());
        assertFalse(emptyEitherC.isNothing());
        assertFalse(emptyEitherC.getA().isPresent());
        assertFalse(emptyEitherC.getB().isPresent());
        assertTrue(emptyEitherC.getC().isPresent());

        assertFalse(nothingEitherC.isSuccess());
        assertFalse(nothingEitherC.isFailure());
        assertFalse(nothingEitherC.isUnknown());
        assertFalse(nothingEitherC.isEmpty());
        assertTrue(nothingEitherC.isNothing());
        assertFalse(nothingEitherC.getA().isPresent());
        assertFalse(nothingEitherC.getB().isPresent());
        assertTrue(nothingEitherC.getC().isPresent());

        assertFalse(unknownEitherC.isSuccess());
        assertFalse(unknownEitherC.isFailure());
        assertTrue(unknownEitherC.isUnknown());
        assertFalse(unknownEitherC.isNothing());
        assertFalse(unknownEitherC.isEmpty());
        assertFalse(unknownEitherC.getA().isPresent());
        assertFalse(unknownEitherC.getB().isPresent());
        assertTrue(unknownEitherC.getC().isPresent());
    }

    @Test
    public void eitherSuccessOrFailureMethods() {
        Container<Boolean> runnableStatus = Container.of(Boolean.FALSE);

        Supplier<Integer> okSupplier = () -> 1;
        Runnable okRunnable = () -> runnableStatus.value = Boolean.TRUE;

        Supplier<Integer> nullSupplier = () -> null;

        Supplier<Integer> failureSupplier = () -> {
            throw new RuntimeException("A");
        };
        Runnable failureRunnable = () -> {
            throw new RuntimeException("B");
        };

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
                success -> {
                },
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
                success -> {
                },
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
        Supplier<Integer> failureSupplier = () -> {
            throw new RuntimeException("A");
        };

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
                nothing -> {
                },
                failure -> fail("no failure expected")
        );

        either3Failure.on(
                integer -> fail("no integer expected"),
                nothing -> fail("no nothing expected"),
                failure -> assertEquals("A", failure.getThrowable().get().getMessage())
        );

        eitherIntegerSuccess.on(
                success -> {
                },
                failure -> fail("no failure expected")
        );

        eitherNothingFailure.on(
                success -> {
                },
                failure -> fail("no failure expected")
        );

        eitherFailureFailure.on(
                success -> fail("no success expected"),
                failure -> assertEquals("A", failure.getThrowable().get().getMessage())
        );
    }

    @Test
    public void eitherGetOrElse() {
        Either<Integer, String> eitherA = Either.a(1);
        Either<Integer, String> eitherB = Either.b("a");

        Either3<Integer, Double, String> either3A = Either3.a(1);
        Either3<Integer, Double, String> either3B = Either3.b(2.0);
        Either3<Integer, Double, String> either3C = Either3.c("a");

        assertEquals(1, eitherA.getAOrElse(37));
        assertEquals("test", eitherA.getBOrElse("test"));

        assertEquals(37, eitherB.getAOrElse(37));
        assertEquals("a", eitherB.getBOrElse("test"));

        assertEquals(1, either3A.getAOrElse(37));
        assertEquals(5.0, either3A.getBOrElse(5.0));
        assertEquals("test", either3A.getCOrElse("test"));

        assertEquals(37, either3B.getAOrElse(37));
        assertEquals(2.0, either3B.getBOrElse(5.0));
        assertEquals("test", either3B.getCOrElse("test"));

        assertEquals(37, either3C.getAOrElse(37));
        assertEquals(5.0, either3C.getBOrElse(5.0));
        assertEquals("a", either3C.getCOrElse("test"));
    }

    @Test
    public void eitherGetOrThrow() {
        Either<Integer, String> eitherA = Either.a(1);
        Either<Integer, String> eitherB = Either.b("a");

        Either<Failure, Integer> failureAEither = Either.a(Failure.of(new MySpecialTestException("test1A")));
        Either<Integer, Failure> failureBEither = Either.failure("m1B", new MySpecialTestException("test1B"));

        Either<Integer, Failure> messageEither = Either.failure("message");

        assertEquals(1, eitherA.getAOrThrow());
        assertThrows(NoSuchConstituentException.class, eitherA::getBOrThrow);

        assertThrows(NoSuchConstituentException.class, eitherB::getAOrThrow);
        assertEquals("a", eitherB.getBOrThrow());

        try {
            failureAEither.getBOrThrow();
            fail("this line must not be reached");
        } catch (Exception e) {
            assertEquals(NoSuchConstituentException.class, e.getClass());
            assertEquals(MySpecialTestException.class, e.getCause().getClass());
            assertEquals(MySpecialTestException.class.getName() + ": " + "test1A", e.getMessage());
            assertEquals(e.getCause().getMessage(), "test1A");
        }

        try {
            failureBEither.getAOrThrow();
            fail("this line must not be reached");
        } catch (Exception e) {
            assertEquals(NoSuchConstituentException.class, e.getClass());
            assertEquals(MySpecialTestException.class, e.getCause().getClass());
            assertEquals("m1B", e.getMessage());
            assertEquals(e.getCause().getMessage(), "test1B");
        }

        try {
            messageEither.getAOrThrow();
            fail("this line must not be reached");
        } catch (Exception e) {
            assertEquals(NoSuchConstituentException.class, e.getClass());
            assertEquals("message", e.getMessage());
            assertNull(e.getCause());
        }
    }

    @Test
    public void either3GetOrThrow() {
        Either3<Integer, Float, String> either3A = Either3.a(10);
        Either3<Integer, Float, String> either3B = Either3.b(2.0f);
        Either3<Integer, Float, String> either3C = Either3.c("x");

        Either3<Failure, Integer, Double> failureAEither3 = Either3.a(Failure.of(new MySpecialTestException("test2A")));
        Either3<Integer, Failure, Double> failureBEither3 = Either3.b(Failure.of(new MySpecialTestException("test2B")));
        Either3<Integer, Double, Failure> failureCEither3 = Either3.failure(new MySpecialTestException("test2C"));

        assertEquals(10, either3A.getAOrThrow());
        assertThrows(NoSuchConstituentException.class, either3A::getBOrThrow);
        assertThrows(NoSuchConstituentException.class, either3A::getCOrThrow);

        assertThrows(NoSuchConstituentException.class, either3B::getAOrThrow);
        assertEquals(2.0f, either3B.getBOrThrow());
        assertThrows(NoSuchConstituentException.class, either3B::getCOrThrow);

        assertThrows(NoSuchConstituentException.class, either3C::getAOrThrow);
        assertThrows(NoSuchConstituentException.class, either3C::getBOrThrow);
        assertEquals("x", either3C.getCOrThrow());

        try {
            failureAEither3.getBOrThrow();
            fail("this line must not be reached");
        } catch (Exception e) {
            assertEquals(NoSuchConstituentException.class, e.getClass());
            assertEquals(MySpecialTestException.class, e.getCause().getClass());
            assertEquals(e.getCause().getMessage(), "test2A");
        }

        try {
            failureAEither3.getCOrThrow();
            fail("this line must not be reached");
        } catch (Exception e) {
            assertEquals(NoSuchConstituentException.class, e.getClass());
            assertEquals(MySpecialTestException.class, e.getCause().getClass());
            assertEquals(e.getCause().getMessage(), "test2A");
        }

        try {
            failureBEither3.getAOrThrow();
            fail("this line must not be reached");
        } catch (Exception e) {
            assertEquals(NoSuchConstituentException.class, e.getClass());
            assertEquals(MySpecialTestException.class, e.getCause().getClass());
            assertEquals(e.getCause().getMessage(), "test2B");
        }

        try {
            failureBEither3.getCOrThrow();
            fail("this line must not be reached");
        } catch (Exception e) {
            assertEquals(NoSuchConstituentException.class, e.getClass());
            assertEquals(MySpecialTestException.class, e.getCause().getClass());
            assertEquals(e.getCause().getMessage(), "test2B");
        }

        try {
            failureCEither3.getAOrThrow();
            fail("this line must not be reached");
        } catch (Exception e) {
            assertEquals(NoSuchConstituentException.class, e.getClass());
            assertEquals(MySpecialTestException.class, e.getCause().getClass());
            assertEquals(e.getCause().getMessage(), "test2C");
        }

        try {
            failureCEither3.getBOrThrow();
            fail("this line must not be reached");
        } catch (Exception e) {
            assertEquals(NoSuchConstituentException.class, e.getClass());
            assertEquals(MySpecialTestException.class, e.getCause().getClass());
            assertEquals(e.getCause().getMessage(), "test2C");
        }
    }

    @Test
    public void ofString() {
        Either<String, Empty> emptyEitherA = Either.ofString(null);
        Either<String, Empty> emptyEitherB = Either.ofString("  \n \r \t  ");
        Either<String, Empty> valueEither = Either.ofString("  \n \r \t test1  ");

        Either3<String, Empty, Nothing> nothingEither3 = Either3.ofString(null);
        Either3<String, Empty, Nothing> emptyEither3 = Either3.ofString("  \n \r \t  ");
        Either3<String, Empty, Nothing> valueEither3 = Either3.ofString("  \n \r \t test2  ");

        emptyEitherA.on(
                value -> fail("no value expected"),
                empty -> {
                }
        );

        emptyEitherB.on(
                value -> fail("no value expected"),
                empty -> {
                }
        );

        valueEither.on(
                value -> assertEquals("  \n \r \t test1  ", value),
                empty -> fail("no Empty expected")
        );

        nothingEither3.on(
                value -> fail("no value expected"),
                empty -> fail("no Empty expected"),
                nothing -> {
                }
        );

        emptyEither3.on(
                value -> fail("no value expected"),
                empty -> {
                },
                nothing -> fail("no Nothing expected")
        );

        valueEither3.on(
                value -> assertEquals("  \n \r \t test2  ", value),
                empty -> fail("no Empty expected"),
                nothing -> fail("no Nothing expected")
        );
    }

    @Test
    public void toPairs() {
        Either<Integer, Float> e1 = Either.a(1);
        Either<Integer, Float> e2 = Either.b(2.0f);

        Pair<Integer, Float> p1 = e1.toPair();
        Pair<Optional<Integer>, Optional<Float>> p2 = e1.toPairOfOptionals();

        Pair<Integer, Float> p3 = e2.toPair();
        Pair<Optional<Integer>, Optional<Float>> p4 = e2.toPairOfOptionals();

        assertEquals(1, p1.getA());
        assertNull(p1.getB());

        assertEquals(1, p2.getA().get());
        assertFalse(p2.getB().isPresent());

        assertNull(p3.getA());
        assertEquals(2.0f, p3.getB());

        assertFalse(p4.getA().isPresent());
        assertEquals(2.0f, p4.getB().get());
    }

    @Test
    public void toTriples() {
        Either3<Integer, Float, Double> e3 = Either3.a(3);
        Either3<Integer, Float, Double> e4 = Either3.b(4.0f);
        Either3<Integer, Float, Double> e5 = Either3.c(5.0);

        Triple<Integer, Float, Double> t1 = e3.toTriple();
        Triple<Optional<Integer>, Optional<Float>, Optional<Double>> t2 = e3.toTripleOfOptionals();

        Triple<Integer, Float, Double> t3 = e4.toTriple();
        Triple<Optional<Integer>, Optional<Float>, Optional<Double>> t4 = e4.toTripleOfOptionals();

        Triple<Integer, Float, Double> t5 = e5.toTriple();
        Triple<Optional<Integer>, Optional<Float>, Optional<Double>> t6 = e5.toTripleOfOptionals();

        assertEquals(3, t1.getA());
        assertNull(t1.getB());
        assertNull(t1.getC());

        assertEquals(3, t2.getA().get());
        assertFalse(t2.getB().isPresent());
        assertFalse(t2.getC().isPresent());

        assertNull(t3.getA());
        assertEquals(4.0f, t3.getB());
        assertNull(t3.getC());

        assertFalse(t4.getA().isPresent());
        assertEquals(4.0f, t4.getB().get());
        assertFalse(t4.getC().isPresent());

        assertNull(t5.getA());
        assertNull(t5.getB());
        assertEquals(5.0, t5.getC());

        assertFalse(t6.getA().isPresent());
        assertFalse(t6.getB().isPresent());
        assertEquals(5.0, t6.getC().get());
    }

    @Test
    public void eitherEquals() {
        Either<Integer, String> e1 = Either.a(1);
        Either<Integer, String> e2 = Either.a(1);
        Either<Integer, String> e3 = Either.a(2);

        Either<Integer, String> e4 = Either.b("a");
        Either<Integer, String> e5 = Either.b("a");
        Either<Integer, String> e6 = Either.b("b");

        Either3<Integer, String, Double> e7 = Either3.a(1);
        Either3<Integer, String, Double> e8 = Either3.b("a");
        Either3<Integer, String, Double> e9 = Either3.c(2.0);

        Either<Either<Integer, String>, Double> r = Either.a(e1);

        assertEquals(e1, e1);
        assertEquals(e4, e4);

        assertEquals(e7, e7);
        assertEquals(e8, e8);
        assertEquals(e9, e9);

        assertNotEquals(1, e1);
        assertNotEquals(e1, 1);
        assertEquals(e1, e2);
        assertEquals(e2, e1);
        assertNotEquals(e1, e3);
        assertNotEquals(e3, e1);

        assertNotEquals("a", e4);
        assertNotEquals(e4, "a");
        assertEquals(e4, e5);
        assertEquals(e5, e4);
        assertNotEquals(e4, e6);
        assertNotEquals(e6, e4);

        assertNotEquals(e1, e4);
        assertNotEquals(e4, e1);

        assertEquals(e1, e7);
        assertEquals(e7, e1);

        assertEquals(e4, e8);
        assertEquals(e8, e4);

        assertNotEquals(e1, e9);
        assertNotEquals(e9, e1);

        assertNotEquals(r, e1);
        assertNotEquals(e1, r);
    }

    @Test
    public void eitherWhileMap() {
        Either<Integer, String> either = Either.a(10);
        Container<Integer> count = Container.of(0);

        Either<Integer, String> result =
                either.whileMap(
                        (e, i) -> i < 10,
                        (e, i) -> {
                            count.value += i;
                            return i > 5
                                    ? Either.a(e.map(k -> k, Integer::parseInt) + 1)
                                    : Either.b(Integer.toString(e.map(k -> k, Integer::parseInt) * 2));
                        });

        result.on(
                a -> assertEquals(644, a),
                b -> fail("no string expected")
        );

        assertEquals(45, count.value);
    }

    @Test
    public void eitherWhileMapAPositive() {
        Either<Integer, Failure> either = Either.a(10);
        Container<Integer> count = Container.of(0);

        Either<Integer, Failure> result =
                either.whileMapA(
                        (e, i) -> e.map(x -> x, f -> -1) > 0,
                        (value, i) -> {
                            count.value += i;
                            return Either.a(value - 1);
                        });

        int value = result.map(x -> x, f -> -2);

        assertEquals(0, value);
        assertEquals(45, count.value);
    }

    @Test
    public void eitherWhileMapANegative() {
        Either<Integer, Failure> either = Either.a(10);
        Container<Integer> count = Container.of(0);

        Either<Integer, Failure> result =
                either.whileMapA(
                        (e, i) -> e.map(x -> x, f -> -1) > 0,
                        (value, i) -> {
                            count.value += i;
                            return value > 5 ? Either.a(value - 1) : Either.failure("failure at " + value);
                        });

        result.on(
                a -> fail("no integer expected"),
                b -> assertEquals("failure at 5", b.getMessage())
        );

        assertEquals(15, count.value);
    }

    @Test
    public void eitherWhileBPositive() {
        Either<String, Integer> either = Either.b(10);
        Container<Integer> count = Container.of(0);

        Either<String, Integer> result =
                either.whileMapB(
                        (e, i) -> e.map(s -> -1, x -> x) > 0,
                        (value, i) -> {
                            count.value += i;
                            return Either.b(value - 1);
                        });

        int value = result.map(s -> -2, x -> x);

        assertEquals(0, value);
        assertEquals(45, count.value);
    }

    @Test
    public void eitherWhileBNegative() {
        Either<String, Integer> either = Either.b(10);
        Container<Integer> count = Container.of(0);

        Either<String, Integer> result =
                either.whileMapB(
                        (e, i) -> e.map(s -> -1, x -> x) > 0,
                        (value, i) -> {
                            count.value += i;
                            return value > 5 ? Either.b(value - 1) : Either.a("failure at " + value);
                        });

        result.on(
                a -> assertEquals("failure at 5", a),
                b -> fail("no integer expected")
        );

        assertEquals(15, count.value);
    }
}
