package org.timux.ports;

import java.lang.reflect.Field;

public class MissingPort {

    public Field field;
    public Object component;

    public MissingPort(Field field, Object component) {
        this.field = field;
        this.component = component;
    }
}