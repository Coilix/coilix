package io.github.coilix.annotation;

import java.lang.annotation.*;

/**
 * Selects a bean by name when several beans match a dependency.
 *
 * <p>Reserved for a future version: Coilix does not read this annotation yet. Use {@link Primary}
 * to resolve ambiguous dependencies.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Documented
public @interface Qualifier {
    /**
     * The name of the bean to inject.
     *
     * @return the bean name
     */
    String value();
}
