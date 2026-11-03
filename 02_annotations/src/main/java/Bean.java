package dev.coilix.annotation;

import java.lang.annotation.*;

/**
 * Declares a bean created by a method of a {@link Configuration} class.
 *
 * <p>The method's parameters are resolved like constructor parameters of a component, and its
 * return value is registered in the application context. Use it for types you cannot annotate, such
 * as classes from other libraries.
 *
 * <pre>
 * {@literal @}Configuration
 * final class ClockConfiguration {
 *   {@literal @}Bean
 *   def clock(): Clock = Clock.systemUTC()
 * }</pre>
 *
 * <p>A method annotated with {@code @Bean} in a class without {@link Configuration} is ignored.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface Bean {
    /**
     * The bean name. When empty, the bean is named after the method.
     *
     * @return the bean name, or an empty string to use the method name
     */
    String value() default "";
}
