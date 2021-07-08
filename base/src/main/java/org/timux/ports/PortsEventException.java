package org.timux.ports;

public class PortsEventException {

    private final Throwable throwable;

    public PortsEventException(Throwable throwable) {
        this.throwable = throwable;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void printStackTrace() {
        throwable.printStackTrace();
    }

    @Override
    public String toString() {
        return "PortsEventException{" +
                "throwable=" + throwable +
                '}';
    }
}
