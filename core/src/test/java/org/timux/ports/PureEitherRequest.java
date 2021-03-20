package org.timux.ports;

import org.timux.ports.types.Failure;

import java.util.Objects;

@Response(Integer.class)
@Response(Failure.class)
@Pure
public class PureEitherRequest {

    private final int arg;
    private final int arg2 = 1;

    public PureEitherRequest(int arg) {
        this.arg = arg;
    }

    public int getArg() {
        return arg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PureEitherRequest that = (PureEitherRequest) o;
        return arg == that.arg;
    }

    @Override
    public int hashCode() {
        return Objects.hash(arg);
    }
}
