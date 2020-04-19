package org.timux.ports;

import java.lang.annotation.*;

/**
 * Use this annotation to declare a response type for a {@link Request} type.
 * This declaration will serve as a hint for both the application developer
 * and the Ports Framework.
 * <p>
 * Starting with Ports 0.4.1, you may use up to 3 of these annotations at once on a
 * request type. This indicates the use of a union type as a response type. In this
 * case, your requests must return an {@link Either} (2 annotations) or
 * an {@link Either3} (3 annotations).
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
