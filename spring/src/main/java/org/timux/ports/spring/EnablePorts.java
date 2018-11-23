package org.timux.ports.spring;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures a Spring application so that Ports can be used. This annotation is not
 * required if Spring is not used.
 *
 * @author Tim Rohlfs
 *
 * @since 0.2.2
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Configuration
@Import(PortsConfiguration.class)
public @interface EnablePorts {

}
