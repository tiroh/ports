package org.timux.ports.testapp.component;

public class StringEvent {

    private final String string;

    public StringEvent(String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }
}
