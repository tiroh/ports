package org.timux.ports.spring;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UiWatchdog implements
        VaadinServiceInitListener,
        SessionInitListener,
        SessionDestroyListener,
        UIInitListener,
        Runnable
{
    private final static Logger logger = LoggerFactory.getLogger(UiWatchdog.class);

    private Map<VaadinService, Set<UI>> serviceUiMap = new HashMap<>();

    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static PortConnector portConnector = null;

    @Override
    public void serviceInit(ServiceInitEvent serviceInitEvent) {
        VaadinService service = serviceInitEvent.getSource();

        logger.debug("register service {} {}", service, this);

        serviceUiMap.put(service, new HashSet<>());

        service.addSessionInitListener(this);
        service.addSessionDestroyListener(this);
        service.addUIInitListener(this);

        scheduler.scheduleAtFixedRate(this, 5, 5, TimeUnit.SECONDS);
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

        serviceUiMap.get(uiInitEvent.getSource()).add(uiInitEvent.getUI());
    }

    @Override
    public void run() {
        serviceUiMap.forEach((service, uis) -> {
            logger.debug("registry status: {} {}", service.getServiceName(), uis.toString());
        });
    }

    public static void setPortConnector(PortConnector portConnector) {
        UiWatchdog.portConnector = portConnector;
    }
}