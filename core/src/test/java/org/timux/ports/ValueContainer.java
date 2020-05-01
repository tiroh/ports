package org.timux.ports;

public class ValueContainer<T> {

    public T value;

    public ValueContainer(T defaultValue) {
        value = defaultValue;
    }
}