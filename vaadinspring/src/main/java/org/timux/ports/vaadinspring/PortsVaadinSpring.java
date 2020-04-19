package org.timux.ports.vaadinspring;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * A utility class for functionality specific for the Vaadin/Spring tandem.
 *
 * @since 0.4.0
 */
@Component
public final class PortsVaadinSpring {

    private final ApplicationContext applicationContext;

    private static PortsVaadinSpring self;

    public PortsVaadinSpring(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    private void init() {
        self = this;
    }

    /**
     * Checks whether all {@link org.timux.ports.Request} ports of all instantiated components are connected.
     *
     * @throws org.timux.ports.PortNotConnectedException If there is a Request port that is not connected.
     */
    public static void verify() {
        PortConnector portConnector = self.applicationContext.getBean(PortConnector.class);
        portConnector.verify();
    }
}
