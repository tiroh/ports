package org.timux.ports;

public class InvalidResponseDeclarationException extends RuntimeException {

    public InvalidResponseDeclarationException(String requestType) {
        super("request type '" + requestType + "' provides an invalid response type declaration");
    }
}
