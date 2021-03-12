package org.timux.ports;

import org.timux.ports.types.Either;
import org.timux.ports.types.Failure;

public class F {

    @In
    private Either<Integer, Failure> onEitherXFailureRequest(EitherXFailureRequest request) {
        throw new MySpecialTestException(request.getMessage());
    }
}
