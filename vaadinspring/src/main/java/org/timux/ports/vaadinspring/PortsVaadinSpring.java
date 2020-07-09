/*
 * Copyright 2018-2020 Tim Rohlfs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.timux.ports.vaadinspring;

import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.timux.ports.Ports;
import org.timux.ports.types.Either;
import org.timux.ports.types.Unknown;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebListener;

/**
 * A utility class for functionality specific for the Vaadin/Spring tandem.
 *
 * @since 0.4.0
 */
@Component
@WebListener
public final class PortsVaadinSpring implements ApplicationListener<ApplicationReadyEvent> {

    private final ApplicationContext applicationContext;

    private static PortsVaadinSpring self;

    private boolean greetingHasBeenDisplayed = false;

    public PortsVaadinSpring(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    private void init() {
        self = this;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!greetingHasBeenDisplayed) {
            greetingHasBeenDisplayed = true;
            LoggerFactory.getLogger(PortsVaadinSpring.class).info("Running with Ports {}", Ports.getVersionString());
        }
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

    /**
     * Returns the username of the user (w.r.t. Spring Security) that "owns" the provided component.
     *
     * <p> A user "owns" a component if it was created by a thread that runs in that user's security context.
     */
    public static Either<String, Unknown> ownerOf(Object component) {
        String owner = self.applicationContext.getBean(ComponentOwnershipRegistry.class).getOwner(component);
        return owner != null ? Either.a(owner) : Either.b(Unknown.INSTANCE);
    }

    /**
     * Returns true if and only if the provided component is "owned" by the user with the provided
     * username (w.r.t. Spring Security).
     *
     * <p> A user "owns" a component if it was created by a thread that runs in that user's security context.
     */
    public static boolean isComponentOwnedBy(Object component, String username) {
        return ownerOf(component).map(
                username::equals,
                unknown -> false
        );
    }
}
