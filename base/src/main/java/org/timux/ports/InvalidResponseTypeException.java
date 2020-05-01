package org.timux.ports;

public class InvalidResponseTypeException extends RuntimeException {

    public InvalidResponseTypeException(String providedType, String requestType) {
        super("request type '" + requestType + "' does not respond with '" + providedType + "'");
    }
}
