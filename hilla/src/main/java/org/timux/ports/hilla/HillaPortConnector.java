/*
 * Copyright 2022 Tim Rohlfs
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

package org.timux.ports.hilla;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.stereotype.Component;
import org.timux.ports.Ports;

import java.lang.reflect.InaccessibleObjectException;

@Component
public class HillaPortConnector implements DestructionAwareBeanPostProcessor {

  private static final Logger logger = LoggerFactory.getLogger(HillaPortConnector.class);

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    try {
      Ports.register(bean);
      logger.debug("registered bean {}", beanName);
    } catch (InaccessibleObjectException ignored) {
    }

    return DestructionAwareBeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
  }

  @Override
  public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
    Ports.unregister(bean);
    logger.debug("unregistered bean {}", beanName);
  }
}