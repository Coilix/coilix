package dev.coilix.annotation;

import java.lang.annotation.*;

/**
 * Sets the order in which {@code ApplicationRunner} beans run after startup.
 *
 * <p>Runners with a lower value run first. Runners without the annotation run last.
 *
 * <pre>
 * {@literal @}Component
 * {@literal @}Order(1)
 * final class MigrationRunner extends ApplicationRunner { ... }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface Order {
    /** The value that makes a runner run before all others. */
    int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;

    /** The value that makes a runner run after all others. Used when the annotation is missing. */
    int LOWEST_PRECEDENCE = Integer.MAX_VALUE;

    /**
     * The position of the runner. Lower values run first.
     *
     * @return the order value
     */
    int value() default LOWEST_PRECEDENCE;
}
