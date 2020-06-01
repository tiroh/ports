package org.timux.ports;

import org.timux.ports.types.Either;
import org.timux.ports.types.Either3;
import org.timux.ports.types.Failure;
import org.timux.ports.types.Nothing;

public class EitherB {

    @In
    private Either<Integer, Failure> onEitherXFailureRequest(EitherXFailureRequest request) {
        throw new MySpecialTestException(request.getMessage());
    }

    @In
    private Either<Integer, String> onEitherXYRequest(EitherXYRequest request) {
        throw new MySpecialTestException(request.getMessage());
    }

    @In
    private Either3<Integer, Nothing, Failure> onEither3XYFailureRequest(Either3XYFailureRequest request) {
        throw new MySpecialTestException(request.getMessage());
    }

    @In
    private Either3<Integer, Nothing, String> onEither3XYZRequest(Either3XYZRequest request) {
        throw new MySpecialTestException(request.getMessage());
    }
}
