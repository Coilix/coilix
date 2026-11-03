package dev.coilix.annotation;

import java.lang.annotation.*;

/**
 * Marks the constructor that Coilix uses to create a bean.
 *
 * <p>Coilix injects dependencies only through constructors. When a bean class has several public
 * constructors, the one annotated with {@code @Autowired} is used; without the annotation Coilix
 * picks the public constructor with the most parameters.
 *
 * <p>Classes created from configuration, such as {@link ConfigurationProperties} classes and
 * components chosen with a {@code class} key, follow a stricter rule: they need a single public
 * constructor, or exactly one marked with {@code @Autowired}. Otherwise binding fails.
 *
 * <pre>
 * {@literal @}Service
 * final class OrderService(repository: OrderRepository, clock: Clock) {
 *   {@literal @}Autowired
 *   def this(repository: OrderRepository) = this(repository, Clock.systemUTC())
 * }</pre>
 *
 * <p>The annotation is accepted on fields and methods for source compatibility, but Coilix ignores
 * it there.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD})
@Documented
public @interface Autowired {}
