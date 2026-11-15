package dev.coilix.annotation;

import java.lang.annotation.*;

/**
 * Gives a case of an {@link Enumerated} enum a name other than the one in the code, and optionally
 * older names that are still accepted.
 *
 * <pre>
 * {@literal @}Enumerated
 * enum Plan {
 *   {@literal @}EnumName(value = "basic", aliases = Array("free")) case Basic
 *   {@literal @}EnumName("premium") case Premium
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Documented
public @interface EnumName {
    /**
     * The name used to read and write the case.
     *
     * @return the name of the case
     */
    String value();

    /**
     * Other names that also find the case when reading, for example names used before a rename.
     *
     * @return the accepted older names
     */
    String[] aliases() default {};
}
