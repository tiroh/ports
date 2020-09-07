package org.timux.ports;

@Response(Integer.class)
public class IocTestRequest {

    private final int data;

    public IocTestRequest(int data) {
        this.data = data;
    }

    public int getData() {
        return data;
    }
}
