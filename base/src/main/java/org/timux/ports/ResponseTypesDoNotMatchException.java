package org.timux.ports;

public class ResponseTypesDoNotMatchException extends RuntimeException {

    public ResponseTypesDoNotMatchException(String requestType, String responseType, String providedResponseType) {
        super(String.format("request type '%s' declares response type '%s', but '%s' is assumed",
                requestType, responseType, providedResponseType));
    }
}
