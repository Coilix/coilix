package dev.coilix.annotation;

import java.lang.annotation.*;

/**
 * Adds a component or a {@link Bean} method to the context only when no other bean provides its
 * type.
 *
 * <p>Use it for defaults that an application can replace: when the application defines its own bean
 * of the same type, or a subtype, the default is left out. It is meant for beans of optional
 * modules, which the application does not control.
 *
 * <pre>
 * {@literal @}Bean
 * {@literal @}ConditionalOnMissingBean
 * def clock: Clock = Clock.systemUTC()</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface ConditionalOnMissingBean {}
