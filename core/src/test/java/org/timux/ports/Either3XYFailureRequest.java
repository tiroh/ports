package org.timux.ports;

import org.timux.ports.types.Failure;
import org.timux.ports.types.Nothing;

@Response(Integer.class)
@Response(Nothing.class)
@Response(Failure.class)
public class Either3XYFailureRequest {

    private final String message;

    public Either3XYFailureRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
