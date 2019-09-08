package org.timux.ports.testapp;

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
        H h = new H();
        J j = new J();

        Ports.connect(a).and(b, PortsOptions.DO_NOT_ALLOW_MISSING_PORTS);
        Ports.connect(c).and(d);
        Ports.connect(c).and(e, PortsOptions.DO_NOT_ALLOW_MISSING_PORTS);
        Ports.connect(f).and(g, PortsOptions.DO_NOT_ALLOW_MISSING_PORTS);
        Ports.connect(h).and(j);

        Ports.verify(a, b, c, d, e, f, g, h, j);

        System.out.println(String.format("Main thread is %d.", Thread.currentThread().getId()));

        a.doWork();
        c.doStringWork();
        c.doIntWork();
        f.doWork();
        h.doWork();

        Ports.disconnect(c).and(d);
        Ports.connect(c).and(e);

        Ports.verify(c, e);

        c.doStringWork();
        c.doIntWork();

        Ports.shutdown();
    }
}
