package org.timux.ports;

public class D {

    @Out
    private Request<EitherRequest, Either<Double, String>> eitherRequest;

    @Out
    private Request<Either3Request, Either3<Double, Integer, String>> either3Request;
}
