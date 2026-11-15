package dev.coilix.annotation;

import java.lang.annotation.*;

/**
 * Marks a class that wraps exactly one value, such as an identifier, an amount or an e-mail
 * address.
 *
 * <p>Coilix reads and writes such a class as its inner value. In the configuration, {@code port:
 * 8080} becomes {@code Port(8080)}. The class needs one public constructor with one parameter, and
 * a public field or method with the same name as that parameter. A check in the constructor, such
 * as {@code require}, rejects invalid values when Coilix creates the class.
 *
 * <pre>
 * {@literal @}ValueType
 * final case class Port(value: Int) {
 *   require(value &gt; 0 &amp;&amp; value &lt; 65536, s"Port must be between 1 and 65535, but is $value")
 * }</pre>
 *
 * <p>Do not extend {@code AnyVal}. The JVM sees a parameter of such a class as its inner value, so
 * Coilix cannot recognize it and the constructor check does not run.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface ValueType {}
