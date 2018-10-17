package org.timux.ports.spring;

import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
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
public class PortConnector implements DestructionAwareBeanPostProcessor, BeanFactoryPostProcessor {

    private final static String SINGLETON_SCOPE = "singleton";
    private final static String APPLICATION_SCOPE = "application";
    private final static String SESSION_SCOPE = "session";
    private final static String VAADIN_SESSION_SCOPE = "vaadin-session";
    private final static String UI_SCOPE = "vaadin-ui";
    private final static String VAADIN_VIEW_SCOPE = "vaadin-view";
    private final static String PROTOTYPE_SCOPE = "prototype";

    private final static Logger logger = LoggerFactory.getLogger(PortConnector.class);

    private Map<String, Object> beans = new HashMap<>();

//    private Map<Object, Scope> beans = new HashMap<>();

    private Scope rootScope = new Scope("root");

    private final Map<String, Integer> SCOPE_ORDERING = new HashMap<>();

    private ConfigurableListableBeanFactory beanFactory;

    public PortConnector() {
        SCOPE_ORDERING.put(APPLICATION_SCOPE, 0);
        SCOPE_ORDERING.put(SESSION_SCOPE, 1);
        SCOPE_ORDERING.put(VAADIN_SESSION_SCOPE, 2);
        SCOPE_ORDERING.put(UI_SCOPE, 3);
        SCOPE_ORDERING.put(VAADIN_VIEW_SCOPE, 4);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        beanFactory = configurableListableBeanFactory;
    }

//    @Override
//    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
//        boolean isPortsComponent = false;
//
//        for (Field field : bean.getClass().getDeclaredFields()) {
//            Out outAnno = field.getAnnotation(Out.class);
//            In inAnno = field.getAnnotation(In.class);
//
//            if (outAnno != null || inAnno != null) {
//                isPortsComponent = true;
//                break;
//            }
//        }
//
//        if (!isPortsComponent) {
//            for (Method method : bean.getClass().getDeclaredMethods()) {
//                In inAnno = method.getAnnotation(In.class);
//
//                if (inAnno != null) {
//                    isPortsComponent = true;
//                    break;
//                }
//            }
//        }
//
//        if (isPortsComponent) {
//            String beanScopeName;
//
//            try {
//                beanScopeName = beanFactory.getBeanDefinition(beanName).getScope();
//            } catch (NoSuchBeanDefinitionException e) {
//                if (UI.getCurrent() != null) {
//                    beanScopeName = UI_SCOPE;
//                } else if (VaadinSession.getCurrent() != null) {
//                    beanScopeName = VAADIN_SESSION_SCOPE;
//                } else {
//                    return bean;
//                }
//            }
//
//            Scope beanScope = getScope(beanScopeName, bean);
//            Scope currentScope = beanScope;
//
//            do {
//                connectBeans(currentScope, bean, beanName);
//                currentScope = currentScope.getParentScope();
//            } while (currentScope != null);
//
//            connectChildScopes(beanScope, bean, beanName);
//
//            beanScope.addBean(bean, beanName);
//            beans.put(bean, beanScope);
//        }
//
//        return bean;
//    }
//
//    @Override
//    public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
//        Scope beanScope = beans.get(bean);
//
//        if (beanScope != null) {
//            Scope currentScope = beanScope;
//
//            do {
//                disconnectBeans(currentScope, bean, beanName);
//                currentScope = currentScope.getParentScope();
//            } while (currentScope != null);
//
//            disconnectChildScopes(beanScope, bean, beanName);
//
//            beans.remove(bean);
//        }
//    }
//
//    private void connectChildScopes(Scope scope, Object bean, String beanName) {
//        for (Scope childScope : scope.getChildScopes()) {
//            connectChildScopes(childScope, bean, beanName);
//            connectBeans(childScope, bean, beanName);
//        }
//    }
//
//    private void disconnectChildScopes(Scope scope, Object bean, String beanName) {
//        for (Scope childScope : scope.getChildScopes()) {
//            disconnectChildScopes(childScope, bean, beanName);
//            disconnectBeans(childScope, bean, beanName);
//        }
//    }
//
//    private void connectBeans(Scope scope, Object bean, String beanName) {
//        for (Map.Entry<String, Object> e : scope.getBeans()) {
//            if (!beanName.equals(e.getKey())) {
//                logger.debug("Connecting ports: {} <-> {}", beanName, e.getKey());
//                Ports.connect(bean).and(e.getValue(), PortsOptions.FORCE_CONNECT_ALL);
//            }
//        }
//    }
//
//    private void disconnectBeans(Scope scope, Object bean, String beanName) {
//        for (Map.Entry<String, Object> e : scope.getBeans()) {
//            if (!beanName.equals(e.getKey())) {
//                logger.debug("Disconnecting ports: {} <-> {}", beanName, e.getKey());
//                Ports.disconnect(bean).and(e.getValue());
//            }
//        }
//    }

    private Scope getScope(String scopeName, Object bean) {
        Scope scope = rootScope.getChildScope(SINGLETON_SCOPE);

        Integer scopeOrder = SCOPE_ORDERING.get(scopeName);

        if (scopeOrder != null) {
            scope = scope.getChildScope(APPLICATION_SCOPE);

            if (scopeOrder >= 1) {
                scope = scope.getChildScope(SESSION_SCOPE);
            }

            if (scopeOrder >= 2) {
                scope = scope.getChildScope(VAADIN_SESSION_SCOPE).getChildScope(VaadinSession.getCurrent());
            }

            if (scopeOrder >= 3) {
                UI ui = UI.getCurrent();

                if (ui == null) {
                    if (bean instanceof com.vaadin.ui.Component) {
                        ui = ((com.vaadin.ui.Component) bean).getUI();
                    }
                }

                scope = scope.getChildScope(UI_SCOPE).getChildScope(ui.getUIId());
            }

            if (scopeOrder >= 4) {
                scope = scope.getChildScope(VAADIN_VIEW_SCOPE);
            }
        } else {
            switch (scopeName) {
                case SINGLETON_SCOPE:
                    break;

                case PROTOTYPE_SCOPE:
                    scope = scope.getChildScope(PROTOTYPE_SCOPE);
                    break;

                default:
                    throw new IllegalStateException("unhandled scope: " + scopeName);
            }
        }

        return scope;
    }

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