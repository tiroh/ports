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
 
package org.timux.ports.testapp;

import org.timux.ports.DispatchPolicy;
import org.timux.ports.SyncPolicy;
import org.timux.ports.Ports;
import org.timux.ports.PortsOptions;
import org.timux.ports.testapp.component.*;

public class TestApplication {

    public static void main(String[] args) {
        A a = new A();
        B b = new B();
        C c = new C();
        D d = new D();
        E e = new E();
        F f = new F();
        G g = new G();

        Ports.domain("test-a", SyncPolicy.NO_SYNC, DispatchPolicy.PARALLEL)
                .addComponents(a);

        Ports.domain("test-b", SyncPolicy.NO_SYNC, DispatchPolicy.PARALLEL)
                .addComponents(b);

        Ports.connect(a).and(b);
        Ports.connect(b).and(c);
        Ports.connect(c).and(d);
        Ports.connect(c).and(e, PortsOptions.DO_NOT_ALLOW_MISSING_PORTS);
        Ports.connect(f).and(g, PortsOptions.DO_NOT_ALLOW_MISSING_PORTS);

        Ports.verify(a, b, c, d, e, f, g);

        a.doWork();
        c.doStringWork();
        c.doIntWork();
        f.doWork();

        Ports.disconnect(c).and(d);
        Ports.connect(c).and(e);

        Ports.verify(c, e);

        c.doStringWork();
        c.doIntWork();

        System.out.println("--- done ---");
    }
}
