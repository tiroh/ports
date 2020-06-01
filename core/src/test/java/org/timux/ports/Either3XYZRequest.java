package org.timux.ports;

import org.timux.ports.types.Nothing;

@Response(Integer.class)
@Response(Nothing.class)
@Response(String.class)
public class Either3XYZRequest {

    private final String message;

    public Either3XYZRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
