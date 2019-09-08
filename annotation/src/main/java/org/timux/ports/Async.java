package org.timux.ports;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Async {

    int SL_CLASS = 1;
    int SL_PORT = 2;

    int multiplicity() default 1;
    int syncLevel() default SL_CLASS;
}
