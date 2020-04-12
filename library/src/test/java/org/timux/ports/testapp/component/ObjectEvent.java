package org.timux.ports.testapp.component;

public class ObjectEvent {

    private final Object object;

    public ObjectEvent(Object object) {
        this.object = object;
    }

    public Object getObject() {
        return object;
    }
}
