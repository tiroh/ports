package org.timux.ports;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that a component is an asynchronous component, i.e. that it handles
 * IN port signals concurrently.
 *
 * @see SyncLevel
 *
 * @author Tim Rohlfs
 * @since 0.4.0
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Async {

    /**
     * Defines how many concurrent signals the component shall be able to handle.
     * This setting has a real effect only if {@link Async#syncLevel()} is set
     * to something different than {@link SyncLevel#COMPONENT}. However, be VERY careful
     * with that setting (see also {@link SyncLevel}).
     */
    int multiplicity() default 1;

    /**
     * Defines the synchronization level of the component, as defined in {@link SyncLevel}.
     * It is strongly recommended to always use the default setting, which is
     * {@link SyncLevel#COMPONENT}. See {@link SyncLevel} for more information.
     */
    int syncLevel() default SyncLevel.COMPONENT;
}
