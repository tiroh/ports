package org.timux.ports;

public class IocCompB {

    @In
    private Integer onIocTestRequest(IocTestRequest request) {
        return request.getData() * 2;
    }
}
