package dev.coilix.annotation;

import java.lang.annotation.*;

/**
 * Injects a configuration value into a constructor or {@link Bean} method parameter.
 *
 * <p>The value is either a placeholder {@code ${key}} or {@code ${key:default}}, resolved from the
 * application configuration, or a literal. Both are converted to the parameter type: strings,
 * numbers, booleans, {@code java.time.Duration} and {@code FiniteDuration} are supported.
 *
 * <pre>
 * {@literal @}Component
 * final class Greeter(@Value("${greeting.text:Hello}") text: String)</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@Documented
public @interface Value {
    /**
     * A placeholder such as {@code ${server.port:8080}} or a literal value.
     *
     * @return the value expression
     */
    String value();
}
