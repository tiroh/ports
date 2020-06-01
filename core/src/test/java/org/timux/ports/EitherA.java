package org.timux.ports;

import org.timux.ports.types.Either;
import org.timux.ports.types.Either3;
import org.timux.ports.types.Failure;
import org.timux.ports.types.Nothing;

public class EitherA {

    @Out
    Request<EitherXFailureRequest, Either<Integer, Failure>> eitherXFailureRequest;

    @Out
    Request<EitherXYRequest, Either<Integer, String>> eitherXYRequest;

    @Out
    Request<Either3XYFailureRequest, Either3<Integer, Nothing, Failure>> either3XYFailureRequest;

    @Out
    Request<Either3XYZRequest, Either3<Integer, Nothing, String>> either3XYZRequest;
}
