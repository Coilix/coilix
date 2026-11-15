package dev.coilix.annotation;

import java.lang.annotation.*;

/**
 * Marks an enum whose cases Coilix reads and writes by name, for example in the configuration.
 *
 * <p>The annotation works on a Scala 3 {@code enum} whose cases have no parameters. Each case is
 * written as its name, unless {@link EnumName} gives it another one.
 *
 * <pre>
 * {@literal @}Enumerated
 * enum Mode {
 *   case Fast, Safe
 *   {@literal @}EnumName("read-only") case ReadOnly
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface Enumerated {
    /**
     * Whether a name matches a case regardless of letter case, so that {@code fast} also finds
     * {@code Fast}.
     *
     * @return {@code true} to ignore letter case when looking up a case by name
     */
    boolean ignoreCase() default false;
}
