package org.timux.ports.testapp.component;

public class ObjectRequest {

    private final Object object;

    public ObjectRequest(Object object) {
        this.object = object;
    }

    public Object getObject() {
        return object;
    }
}
