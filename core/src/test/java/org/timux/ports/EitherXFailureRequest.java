package org.timux.ports;

import org.timux.ports.types.Failure;

@Response(Integer.class)
@Response(Failure.class)
public class EitherXFailureRequest {

    private final String message;

    public EitherXFailureRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
