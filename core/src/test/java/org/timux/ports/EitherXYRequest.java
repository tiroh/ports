package org.timux.ports;

@Response(Integer.class)
@Response(String.class)
public class EitherXYRequest {

    private final String message;

    public EitherXYRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
