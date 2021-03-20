package org.timux.ports;

import org.timux.ports.types.Either;
import org.timux.ports.types.Failure;

public class PureSender {

    @Out
    private Request<PureEitherRequest, Either<Integer, Failure>> pureEitherRequest;

    public Either<Integer, Failure> runCall(int payload) {
        return pureEitherRequest.call(new PureEitherRequest(payload));
    }

    public Either<Integer, Failure> runCallE(int payload) {
        return pureEitherRequest.callE(new PureEitherRequest(payload)).getAOrThrow();
    }

    public Either<Integer, Failure> runCallF(int payload) {
        return pureEitherRequest.callF(new PureEitherRequest(payload)).get();
    }
}
