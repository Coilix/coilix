package io.github.coilix.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds an SLF4J logger field named {@code log} to the annotated Scala class or object.
 *
 * <p>The field is added at compile time by the {@code coilix-slf4s-plugin} compiler plugin, which
 * must be enabled in the build. The logger is named after the fully qualified class name.
 *
 * <pre>
 * {@literal @}Slf4s
 * final class OrderService {
 *   def place(order: Order): Unit = log.info(s"Placing order ${order.id}")
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Slf4s {}
