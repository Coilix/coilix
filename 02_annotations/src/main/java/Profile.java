package dev.coilix.annotation;

import java.lang.annotation.*;

/**
 * Adds a component or a {@link Bean} method to the context only when one of the given profiles is
 * active.
 *
 * <p>Profiles are listed under {@code coilix.profiles.active}, for example with {@code
 * --coilix.profiles.active=dev}. A name starting with {@code !} matches when that profile is not
 * active. On a {@link Configuration} class, the condition also applies to its {@link Bean} methods.
 *
 * <pre>
 * {@literal @}Component
 * {@literal @}Profile("dev")
 * final class ConsoleMailSender extends MailSender</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface Profile {
    /**
     * The profiles, of which at least one must match.
     *
     * @return profile names, each optionally preceded by {@code !}
     */
    String[] value();
}
