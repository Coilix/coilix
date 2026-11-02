package io.github.coilix.annotation;

import java.lang.annotation.*;

/**
 * Marks a class whose {@link Bean} methods declare additional beans.
 *
 * <p>The class itself is also a bean, created and injected like a {@link Component}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface Configuration {}
