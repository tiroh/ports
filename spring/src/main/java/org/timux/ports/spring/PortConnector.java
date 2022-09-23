/*
 * Copyright 2018-2022 Tim Rohlfs
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

package org.timux.ports.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.timux.ports.In;
import org.timux.ports.MissingPort;
import org.timux.ports.Out;
import org.timux.ports.PortNotConnectedException;
import org.timux.ports.Ports;
import org.timux.ports.PortsOptions;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Component
public class PortConnector implements DestructionAwareBeanPostProcessor, BeanFactoryPostProcessor {

  private static final String ROOT_SCOPE = "root";
  private static final String SINGLETON_SCOPE = ConfigurableBeanFactory.SCOPE_SINGLETON;
  private static final String APPLICATION_SCOPE = WebApplicationContext.SCOPE_APPLICATION;
  private static final String SESSION_SCOPE = WebApplicationContext.SCOPE_SESSION;
  private static final String PROTOTYPE_SCOPE = ConfigurableBeanFactory.SCOPE_PROTOTYPE;

  private static final Logger logger = LoggerFactory.getLogger(PortConnector.class);

  private final Map<Object, Scope> beans = new WeakHashMap<>();
  private final Scope rootScope = new Scope(ROOT_SCOPE);

  private final Map<String, Integer> SCOPE_ORDERING = new HashMap<>();

  private ConfigurableListableBeanFactory beanFactory;

  public PortConnector() {
    SCOPE_ORDERING.put(APPLICATION_SCOPE, 0);
    SCOPE_ORDERING.put(SESSION_SCOPE, 1);
    SCOPE_ORDERING.put(PROTOTYPE_SCOPE, 2);
  }

  @Override
  public synchronized void postProcessBeanFactory(
      ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
    beanFactory = configurableListableBeanFactory;
  }

  @Override
  public synchronized Object postProcessAfterInitialization(Object bean, String beanName)
      throws BeansException {
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
      String beanScopeName;

      try {
        beanScopeName = beanFactory.getBeanDefinition(beanName).getScope();
      } catch (NoSuchBeanDefinitionException e) {
        e.printStackTrace();
        return bean;
      }

      Scope beanScope = getScope(beanScopeName, bean);

      beanScope.addBean(bean, beanName);
      beans.put(bean, beanScope);

      connectParentScopes(beanScope, bean, beanName);
      connectChildScopes(beanScope, bean, beanName);
    }

    return bean;
  }

  @Override
  public synchronized void postProcessBeforeDestruction(Object bean, String beanName)
      throws BeansException {
    Scope beanScope = beans.get(bean);

    if (beanScope != null) {
      logger.debug("removing bean {} due to destruction", beanName);
      disconnectCompletely(beanScope, bean, beanName);
      beanScope.removeBean(bean);
    }
  }

  private synchronized void connectParentScopes(Scope scope, Object bean, String beanName) {
    Scope currentScope = scope;
    boolean isScopeInUi = isScopeInUi(scope);

    do {
      connectBeans(currentScope, bean, beanName, isScopeInUi);
      currentScope = currentScope.getParentScope();
    } while (currentScope != null);
  }

  private synchronized void connectChildScopes(Scope scope, Object bean, String beanName) {
    boolean isScopeInSession = isScopeInUi(scope);

    for (Scope childScope : scope.getChildScopes()) {
      connectChildScopes(childScope, bean, beanName);
      connectBeans(childScope, bean, beanName, isScopeInSession);
    }
  }

  private synchronized void disconnectCompletely(Scope beanScope, Object bean, String beanName) {
    disconnectParentScopes(beanScope, bean, beanName);
    disconnectChildScopes(beanScope, bean, beanName);
    beans.remove(bean);
  }

  private synchronized void disconnectParentScopes(
      Scope currentScope, Object bean, String beanName) {
    do {
      disconnectBeans(currentScope, bean, beanName);
      currentScope = currentScope.getParentScope();
    } while (currentScope != null);
  }

  private synchronized void disconnectChildScopes(Scope scope, Object bean, String beanName) {
    for (Scope childScope : scope.getChildScopes()) {
      disconnectChildScopes(childScope, bean, beanName);
      disconnectBeans(childScope, bean, beanName);
    }
  }

  private synchronized void connectBeans(
      Scope scope, Object bean, String beanName, boolean isBeanScopeInUi) {
    for (Map.Entry<Object, String> e : scope.getBeans()) {
      final Object otherBean = e.getKey();
      final String otherBeanName = e.getValue();

      if (bean == otherBean) {
        // We call this in order to ensure that all ports of the bean are instantiated.
        // (In the rare case that the bean is the only Ports component,
        // its ports would remain uninstantiated.)
        Ports.connectDirected(
            bean, DummyComponent.INSTANCE, PortsOptions.FORCE_CONNECT_EVENT_PORTS);
        continue;
      }

      boolean a;
      boolean b;

      a = Ports.connectDirected(bean, otherBean, PortsOptions.FORCE_CONNECT_EVENT_PORTS | PortsOptions.FAIL_ON_AMBIGUOUS_REQUEST_CONNECTIONS);
      b = Ports.connectDirected(otherBean, bean, PortsOptions.FORCE_CONNECT_EVENT_PORTS | PortsOptions.FAIL_ON_AMBIGUOUS_REQUEST_CONNECTIONS);

      if (a && b) {
        logConnection(bean, beanName, otherBean, otherBeanName, true);
      } else if (a) {
        logConnection(bean, beanName, otherBean, otherBeanName, false);
      } else if (b) {
        logConnection(otherBean, otherBeanName, bean, beanName, false);
      }
    }
  }

  private synchronized void disconnectBeans(Scope scope, Object bean, String beanName) {
    for (Map.Entry<Object, String> e : scope.getBeans()) {
      if (bean != e.getKey()) {
        Ports.disconnect(bean).and(e.getKey());
      }
    }
  }

  private synchronized void disconnectChildBeans(Scope scope) {
    scope
        .getBeans()
        .forEach(
            e -> {
              Object bean = e.getKey();
              String beanName = e.getValue();

              disconnectCompletely(scope, bean, beanName);
            });

    scope.removeBeans();

    for (Scope childScope : scope.getChildScopes()) {
      disconnectChildBeans(childScope);
    }

    scope.removeChildScopes();

    //        System.out.println(rootScope.toString());
  }

  private synchronized Scope findScope(Scope parent, Object key) {
    Scope scope = parent.findChildScope(key);

    if (scope != null) {
      return scope;
    }

    for (Scope childScope : parent.getChildScopes()) {
      scope = findScope(childScope, key);

      if (scope != null) {
        return scope;
      }
    }

    return null;
  }

  private synchronized Scope getScope(String scopeName, Object bean) {
    Scope scope = rootScope.getChildScope(SINGLETON_SCOPE);

    Integer scopeOrder = SCOPE_ORDERING.get(scopeName);

    if (scopeOrder != null) {
      scope = scope.getChildScope(APPLICATION_SCOPE);

      if (scopeOrder >= 1) {
        scope = scope.getChildScope(SESSION_SCOPE);
      }

      if (scopeOrder >= 2) {
        scope = scope.getChildScope(PROTOTYPE_SCOPE);
      }
    } else {
      switch (scopeName) {
        case SINGLETON_SCOPE:
          break;

        default:
          throw new IllegalStateException("unhandled scope: " + scopeName);
      }
    }

    return scope;
  }

  private synchronized boolean isScopeInUi(Scope scope) {
    if (SINGLETON_SCOPE.equals(scope.getName()) || ROOT_SCOPE.equals(scope.getName())) {
      return false;
    }

    Integer scopeOrder = SCOPE_ORDERING.get(scope.getName());

    if (scopeOrder != null) {
      return scopeOrder >= 3;
    }

    return isScopeInUi(scope.getParentScope());
  }

  private static void logConnection(
      Object from, String fromName, Object to, String toName, boolean isBidirectional) {
    String operator = isBidirectional ? "<->" : "->";

    logger.debug(
        "Connected ports: {}:{} {} {}:{}",
        fromName,
        Integer.toHexString(from.hashCode()),
        operator,
        toName,
        Integer.toHexString(to.hashCode()));
  }

  synchronized void verify() {
    List<MissingPort> missingPorts = new ArrayList<>();

    Method verifyMethod;

    try {
      verifyMethod = Ports.class.getDeclaredMethod("verifyInternal", boolean.class, Object[].class);
      verifyMethod.setAccessible(true);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }

    verifyScope(rootScope, missingPorts, verifyMethod);

    if (!missingPorts.isEmpty()) {
      throw new PortNotConnectedException(missingPorts);
    }
  }

  @SuppressWarnings("unchecked")
  synchronized void verifyScope(Scope scope, List<MissingPort> missingPorts, Method verifyMethod) {
    scope
        .getBeans()
        .forEach(
            e -> {
              try {
                List<MissingPort> newMissingPorts =
                    (List<MissingPort>) verifyMethod.invoke(null, false, new Object[] {e.getKey()});
                missingPorts.addAll(newMissingPorts);
              } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
              }
            });

    scope.getChildScopes().forEach(s -> verifyScope(s, missingPorts, verifyMethod));
  }
}
