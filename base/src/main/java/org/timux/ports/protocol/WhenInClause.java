package org.timux.ports.protocol;

import org.timux.ports.Protocol;

import java.util.function.Predicate;

public class WhenInClause<T> {

    public ActionClause<T> receives(Predicate<T> predicate) {
        Protocol.registerConditionOnReceived(predicate);
        return new ActionClause<>();
    }
}
