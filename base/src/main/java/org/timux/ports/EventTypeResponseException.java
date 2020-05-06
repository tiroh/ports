package org.timux.ports;

class EventTypeResponseException extends RuntimeException {

    public EventTypeResponseException(String eventType) {
        super("message type '" + eventType + "' is an event, but a response type was provided");
    }
}
