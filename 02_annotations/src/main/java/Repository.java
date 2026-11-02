package io.github.coilix.annotation;

import java.lang.annotation.*;

/**
 * Marks a class that stores or loads data, such as a database access object.
 *
 * <p>Coilix treats it the same as {@link Component}. The separate annotation documents the role of
 * the class.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface Repository {}
