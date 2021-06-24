package org.timux.ports;

import org.timux.ports.types.Either;
import org.timux.ports.types.Failure;

public class PureReceiver {

    @In
    private Either<Integer, Failure> onPureEitherRequest(PureEitherRequest request) {
        return request.getArg() >= 0
                ? Either.a(request.getArg() * 17)
                : Either.failure("is negative: " + request.getArg());
    }

    @In
    private Integer onPureStatelessRequest(PureStatelessRequest request) {
        return 17;
    }
}
