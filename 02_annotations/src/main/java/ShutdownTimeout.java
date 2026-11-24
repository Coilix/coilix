package dev.coilix.annotation;

import java.lang.annotation.*;

/**
 * Sets how long the context waits for the finalizer of one bean made by an effect when it closes,
 * instead of the limit for all of them, such as {@code coilix.cats-effect.shutdown-timeout}.
 *
 * <p>The duration is written like a configuration value, such as {@code "2m"} or {@code "30
 * seconds"}, or as a placeholder such as {@code "${shop.pool-shutdown:2m}"}. A finalizer that takes
 * longer is cancelled and logged, and closing goes on with the other beans. Use it on a {@link
 * Bean} method.
 *
 * <pre>
 * {@literal @}Bean
 * {@literal @}ShutdownTimeout("2m")
 * def connections(settings: DatabaseSettings): Resource[IO, ConnectionPool] =
 *   ConnectionPool.open(settings)</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface ShutdownTimeout {
    /**
     * The longest time the finalizer of the bean may take.
     *
     * @return the duration or a placeholder
     */
    String value();
}
