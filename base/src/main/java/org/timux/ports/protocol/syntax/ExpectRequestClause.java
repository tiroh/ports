package org.timux.ports.protocol.syntax;

import org.timux.ports.protocol.Protocol;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class ExpectRequestClause<I, O> {

    private final Protocol protocol;

    public ExpectRequestClause(Protocol protocol) {
        this.protocol = protocol;
    }

    public ConditionOrAction receives(Predicate<I> predicate) {
        return new ConditionOrAction(protocol);
    }

    public ConditionOrAction returns(BiPredicate<I, O> predicate) {
        return new ConditionOrAction(protocol);
    }


}
