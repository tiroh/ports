package org.timux.ports;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation to declare a success response type for a {@link Request} type.
 * In addition, you will also need a {@link FailureResponse} annotation. Together,
 * these annotations declare a {@link SuccessOrFailure} union response type.
 *
 * @see FailureResponse
 * @see SuccessOrFailure
 * @see Either
 *
 * @author Tim Rohlfs
 * @since 0.4.1
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SuccessResponse {

    Class<?> value();
}
