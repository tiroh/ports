package org.timux.ports;

public class RawUnionTypeException extends RuntimeException {

    public RawUnionTypeException(String requestType, String responseTypeA, String responseTypeB) {
        super("union types must not be used raw (request '" + requestType + "' responds with either '"
                + responseTypeA + "' or '" + responseTypeB + "')");
    }
}
