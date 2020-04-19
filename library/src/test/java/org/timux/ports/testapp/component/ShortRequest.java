package org.timux.ports.testapp.component;

import org.timux.ports.Response;

@Response(Double.class)
public class ShortRequest {

    private final short data;

    public ShortRequest(short data) {
        this.data = data;
    }

    public short getData() {
        return data;
    }
}
