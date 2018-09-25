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

        Ports.connect(a).and(b, PortsOptions.DO_NOT_ALLOW_MISSING_PORTS);
        Ports.connect(c).and(d);
        Ports.connect(c).and(e, PortsOptions.DO_NOT_ALLOW_MISSING_PORTS);
        Ports.connect(f).and(g, PortsOptions.DO_NOT_ALLOW_MISSING_PORTS);

        Ports.verify(a, b, c, d, e, f, g);

        a.doWork();
        c.doStringWork();
        c.doIntWork();
        f.doWork();
    }
}
