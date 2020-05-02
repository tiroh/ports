package org.timux.ports;

@Response(Double.class)
@Response(Integer.class)
@Response(String.class)
public class Either3Request {

    private final int value;

    public Either3Request(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
