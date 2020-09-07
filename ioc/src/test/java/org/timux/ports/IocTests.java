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

package org.timux.ports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.timux.ports.types.Container;

public class IocTests {

    @BeforeEach
    public void beforeEach() {
        PortsApplication.componentScan(getClass().getPackage().getName());
        PortsApplication.start();
    }

    @AfterEach
    public void afterEach() {
        PortsApplication.shutdown();
    }

    @Test
    public void smokeTest() {
        Container<Integer> result = Container.of(0);

        Ports.protocol()
                .when(IocTestRequest.class, Integer.class)
                .responds()
                .do_(response -> result.value = response);

        Ports.protocol()
                .with(IocTestEvent.class)
                .trigger(new IocTestEvent(1));

        Assertions.assertEquals(4, result.value);
    }
}
