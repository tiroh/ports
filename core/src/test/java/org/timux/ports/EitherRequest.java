package org.timux.ports;

@SuccessResponse(Double.class)
@FailureResponse(String.class)
public class EitherRequest {

    private final double value;

    public EitherRequest(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }
}
