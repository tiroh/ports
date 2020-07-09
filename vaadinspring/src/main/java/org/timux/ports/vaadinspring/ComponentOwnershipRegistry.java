package org.timux.ports.vaadinspring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.WeakHashMap;

@Component
public class ComponentOwnershipRegistry implements DestructionAwareBeanPostProcessor {

    private final Map<Object, String> componentOwners = new WeakHashMap<>();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
            componentOwners.put(bean, authentication.getName());
        }

        return bean;
    }

    @Override
    public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
        componentOwners.remove(bean);
    }

    String getOwner(Object component) {
        return componentOwners.get(component);
    }
}
