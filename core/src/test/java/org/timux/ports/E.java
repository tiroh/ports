package org.timux.ports;

import org.timux.ports.types.Either;
import org.timux.ports.types.Either3;

public class E {

    @In
    private Either<Double, String> onEitherRequest(EitherRequest request) {
        return Either.a(request.getValue() * 2.0);
    }

    @In
    private Either3<Double, Integer, String> onEither3Request(Either3Request request) {
        return Either3.a(request.getValue() * 2.5);
    }
}
