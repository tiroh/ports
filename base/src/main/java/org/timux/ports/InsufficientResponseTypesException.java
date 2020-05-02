package org.timux.ports;

public class InsufficientResponseTypesException extends RuntimeException {

    public InsufficientResponseTypesException(String requestType, int requiredParameters, int givenParameters) {
        super("request '" + requestType + "' responds with " + requiredParameters + " types, but only "
            + givenParameters + " were provided");
    }
}
