package org.timux.ports;

public class UninitializedPortException extends RuntimeException {

    public UninitializedPortException(String component, String type) {
        super(String.format("Port <%s> in %s is not initialized. "
                + "(Did you set up the Ports agent correctly?)", type, component));
    }
}
