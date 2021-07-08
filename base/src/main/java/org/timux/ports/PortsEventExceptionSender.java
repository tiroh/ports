package org.timux.ports;

class PortsEventExceptionSender {

    @Out
    private Event<PortsEventException> portsEventException;

    public void trigger(Throwable throwable) {
        if (portsEventException != null && portsEventException.isConnected()) {
            portsEventException.trigger(new PortsEventException(throwable));
        } else {
            throwable.printStackTrace();
        }
    }
}
