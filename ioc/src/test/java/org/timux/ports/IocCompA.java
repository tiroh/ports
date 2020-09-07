package org.timux.ports;

public class IocCompA {

    @Out
    private Request<IocTestRequest, Integer> iocTestRequest;

    @In
    private void onIocTest(IocTestEvent event) {
        iocTestRequest.call(new IocTestRequest(event.getData() + 1));
    }
}
