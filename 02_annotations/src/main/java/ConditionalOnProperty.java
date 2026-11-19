package dev.coilix.annotation;

import java.lang.annotation.*;

/**
 * Adds a component or a {@link Bean} method to the context only when a configuration key has a
 * given value.
 *
 * <p>Without {@link #havingValue()}, any value except {@code false} matches. The comparison ignores
 * letter case.
 *
 * <pre>
 * {@literal @}Component
 * {@literal @}ConditionalOnProperty(name = "shop.audit.enabled", havingValue = "true")
 * final class AuditLog</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface ConditionalOnProperty {
    /**
     * The configuration key, for example {@code shop.audit.enabled}.
     *
     * @return the key whose value is checked
     */
    String name();

    /**
     * The value the key must have, or an empty text to accept any value except {@code false}.
     *
     * @return the expected value
     */
    String havingValue() default "";

    /**
     * Whether the condition matches when the key is not set.
     *
     * @return {@code true} to add the bean when the key is missing
     */
    boolean matchIfMissing() default false;
}
