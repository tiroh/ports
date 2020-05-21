package org.timux.ports;

public class DoubleEvent {

    private final Double data;

    public DoubleEvent(Double data) {
        this.data = data;
    }

    public DoubleEvent(int data) {
        this.data = Double.valueOf(data);
    }

    public Double getData() {
        return data;
    }

    @Override
    public String toString() {
        return Double.toString(data);
    }
}
