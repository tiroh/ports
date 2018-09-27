package org.timux.ports.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.stereotype.Component;
import org.timux.ports.In;
import org.timux.ports.Out;
import org.timux.ports.Ports;
import org.timux.ports.PortsOptions;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Component
public class PortConnector implements DestructionAwareBeanPostProcessor {

    private Map<String, Object> beans = new HashMap<>();

    private final static Logger logger = LoggerFactory.getLogger(PortConnector.class);

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        boolean isPortsComponent = false;

        for (Field field : bean.getClass().getDeclaredFields()) {
            Out outAnno = field.getAnnotation(Out.class);
            In inAnno = field.getAnnotation(In.class);

            if (outAnno != null || inAnno != null) {
                isPortsComponent = true;
                break;
            }
        }

        if (!isPortsComponent) {
            for (Method method : bean.getClass().getDeclaredMethods()) {
                In inAnno = method.getAnnotation(In.class);

                if (inAnno != null) {
                    isPortsComponent = true;
                    break;
                }
            }
        }

        if (isPortsComponent) {
            for (Map.Entry<String, Object> e : beans.entrySet()) {
                if (!beanName.equals(e.getKey())) {
                    logger.debug("Connecting ports: {} <-> {}", beanName, e.getKey());
                    Ports.connect(bean).and(e.getValue(), PortsOptions.FORCE_CONNECT_ALL);
                }
            }

            beans.put(beanName, bean);
        }

        return bean;
    }

    @Override
    public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
        if (beans.containsKey(beanName)) {
            for (Map.Entry<String, Object> e : beans.entrySet()) {
                if (!beanName.equals(e.getKey())) {
                    logger.debug("Disconnecting ports: {} <-> {}", beanName, e.getKey());
                    Ports.disconnect(bean).and(e.getValue());
                }
            }

            beans.remove(beanName);
        }
    }
}