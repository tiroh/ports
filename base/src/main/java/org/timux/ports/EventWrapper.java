package org.timux.ports;

@FunctionalInterface
public interface EventWrapper {

    void execute(PortsCommand f);
}