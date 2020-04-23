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

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.server.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Watches Vaadin's sessions and takes care that Ports components are
 * disconnected whenever a session is destroyed. This avoids memory leaks.
 *
 * @author Tim Rohlfs
 * @since 0.3.0
 */
public class UiWatchdog implements
        VaadinServiceInitListener,
        SessionInitListener,
        SessionDestroyListener,
        UIInitListener,
        ComponentEventListener<DetachEvent>
{
    private final static Logger logger = LoggerFactory.getLogger(UiWatchdog.class);

    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static PortConnector portConnector = null;

    @Override
    public void serviceInit(ServiceInitEvent serviceInitEvent) {
        VaadinService service = serviceInitEvent.getSource();

        logger.debug("init service {}", service);

        service.addSessionInitListener(this);
        service.addSessionDestroyListener(this);
        service.addUIInitListener(this);
    }

    @Override
    public void sessionInit(SessionInitEvent sessionInitEvent) {
        logger.debug("register session {} of service {}",
                sessionInitEvent.getSession().getPushId(),
                sessionInitEvent.getSource().getServiceName());
    }

    @Override
    public void sessionDestroy(SessionDestroyEvent sessionDestroyEvent) {
        logger.debug("unregister session {} of service {}",
                sessionDestroyEvent.getSession().getPushId(),
                sessionDestroyEvent.getSource().getServiceName());

        if (portConnector == null) {
            logger.error("cannot handle session destruction: no PortConnector has been configured");
            return;
        }

        portConnector.onSessionDestroyed(sessionDestroyEvent.getSession());
    }

    @Override
    public void uiInit(UIInitEvent uiInitEvent) {
        logger.debug("register UI {} in session {} of service {}",
                uiInitEvent.getUI().getUIId(),
                uiInitEvent.getUI().getSession().getPushId(),
                uiInitEvent.getSource().getServiceName());

        uiInitEvent.getUI().addDetachListener(this);
    }

    @Override
    public void onComponentEvent(DetachEvent detachEvent) {
        logger.debug("unregister UI {} in session {}", detachEvent.getUI().getUIId(), detachEvent.getUI().getSession().getPushId());

        if (portConnector == null) {
            logger.error("cannot handle UI destruction: no PortConnector has been configured");
            return;
        }

        portConnector.onUiDestroyed(detachEvent.getUI());
    }

    public static void setPortConnector(PortConnector portConnector) {
        UiWatchdog.portConnector = portConnector;
    }
}