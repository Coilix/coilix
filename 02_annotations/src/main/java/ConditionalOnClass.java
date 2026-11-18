package dev.coilix.annotation;

import java.lang.annotation.*;

/**
 * Adds a component or a {@link Bean} method to the context only when the given classes are on the
 * classpath.
 *
 * <p>Classes are named by text, so the annotation itself loads even when a class is missing.
 *
 * <pre>
 * {@literal @}Configuration
 * {@literal @}ConditionalOnClass("org.postgresql.Driver")
 * final class PostgresConfiguration</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface ConditionalOnClass {
    /**
     * The fully qualified names of the classes that must all be present.
     *
     * @return class names such as {@code org.postgresql.Driver}
     */
    String[] value();
}
