package org.timux.ports;

public class MySpecialTestException extends RuntimeException {

    public MySpecialTestException(String message) {
        super(message);
    }

    public MySpecialTestException(String message, Exception e) {
        super(message, e);
    }
}
