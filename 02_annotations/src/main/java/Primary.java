package io.github.coilix.annotation;

import java.lang.annotation.*;

/**
 * Gives a bean precedence when several beans match a requested type.
 *
 * <p>Without {@code @Primary}, injecting a type that has more than one matching bean fails with
 * {@code AmbiguousBeanException}. Use it on a component class or on a {@link Bean} method.
 * Injecting {@code List[T]} still returns all matching beans.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface Primary {}
