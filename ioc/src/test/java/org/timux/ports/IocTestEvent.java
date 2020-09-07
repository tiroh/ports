package org.timux.ports;

public class IocTestEvent {

    private final int data;

    public IocTestEvent(int data) {
        this.data = data;
    }

    public int getData() {
        return data;
    }
}
