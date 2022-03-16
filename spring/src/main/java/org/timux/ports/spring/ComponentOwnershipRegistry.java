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
      synchronized (componentOwners) {
        componentOwners.put(bean, authentication.getName());
      }
    }

    return bean;
  }

  @Override
  public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
    synchronized (componentOwners) {
      componentOwners.remove(bean);
    }
  }

  String getOwner(Object component) {
    synchronized (componentOwners) {
      return componentOwners.get(component);
    }
  }
}
