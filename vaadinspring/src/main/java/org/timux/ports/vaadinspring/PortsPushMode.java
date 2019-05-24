package org.timux.ports.vaadinspring;

public class PortsPushMode {

    static boolean isManual;

    {
        setAutomatic();
    }

    public static void setManual() {
        isManual = true;
    }

    public static void setAutomatic() {
        isManual = false;
    }
}
