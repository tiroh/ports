/*
 * Copyright 2018-2021 Tim Rohlfs
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.timux.ports.types.Container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
    public void iocSmokeTest() {
        IocCompA a = PortsApplication.getOrMakeInstance(IocCompA.class);
        IocCompB b = PortsApplication.getOrMakeInstance(IocCompB.class);

        Ports.register(a, b);

        Container<Integer> result = Container.of(0);

        Ports.protocol()
            .when(IocTestRequest.class, Integer.class)
                .responds()
                .storeIn(result);

        Ports.protocol()
            .with(IocTestEvent.class)
                .trigger(new IocTestEvent(1));

        assertEquals(4, result.value);
    }

    @Test
    public void iocInstantiation() {
        IocCompA stc1 = PortsApplication.getOrMakeInstance(IocCompA.class);
        IocCompA stc2 = PortsApplication.getOrMakeInstance(IocCompA.class);

        IocCompB stc3 = PortsApplication.getOrMakeInstance(IocCompB.class);
        IocCompB stc4 = PortsApplication.getOrMakeInstance(IocCompB.class);

        IocCompC dyn1 = PortsApplication.getOrMakeInstance(IocCompC.class);
        IocCompC dyn2 = PortsApplication.getOrMakeInstance(IocCompC.class);

        assertEquals(stc1, stc2);
        assertEquals(stc3, stc4);

        assertNotEquals(dyn1, dyn2);
    }

    @Test
    public void iocDynamic() {
        IocCompC dyn1 = PortsApplication.getOrMakeInstance(IocCompC.class);
        IocCompC dyn2 = PortsApplication.getOrMakeInstance(IocCompC.class);

        Ports.register(dyn1, dyn2);

        dyn1.testData = 17;
        dyn2.testData = 19;

        Ports.protocol()
            .with(IocTestEvent.class)
                .trigger(new IocTestEvent(1));

        assertEquals(36, dyn1.result);
        assertEquals(40, dyn2.result);
    }
}
