package org.timux.ports.vaadinspring;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class PortsVaadinSpring {

    private final ApplicationContext applicationContext;

    private static PortsVaadinSpring self;

    public PortsVaadinSpring(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        self = this;
    }

    public static void verify() {
        PortConnector portConnector = self.applicationContext.getBean(PortConnector.class);
        portConnector.verify();
    }
}
