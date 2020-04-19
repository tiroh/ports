package org.timux.ports;

import java.lang.annotation.*;

/**
 * Use this annotation to declare a response type for a {@link Request} type.
 * This declaration will serve as a hint for both the application developer
 * and the Ports Framework.
 *
 * @author Tim Rohlfs
 * @since 0.4.0
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Responses.class)
public @interface Response {

    Class<?> value();
}
