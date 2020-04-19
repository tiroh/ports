package org.timux.ports.testapp.component;

import org.timux.ports.Response;

@Response(Object.class)
public class ObjectRequest {

    private final Object object;

    public ObjectRequest(Object object) {
        this.object = object;
    }

    public Object getObject() {
        return object;
    }
}
