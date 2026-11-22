package dev.coilix.annotation;

import java.lang.annotation.*;

/**
 * Makes a bean that is created anew for every lookup, instead of one instance shared by the whole
 * context.
 *
 * <p>Every {@code getBean} call and every {@code get()} of a {@code Provider[T]} returns a new
 * instance. A bean that receives a prototype directly in its constructor gets one instance and
 * keeps it, so a bean that needs a fresh instance each time injects {@code Provider[T]}. The
 * context does not destroy prototypes: their {@code @PreDestroy} methods never run, and closing
 * them is up to the code that took them. Use it on a component class or on a {@link Bean} method.
 *
 * <pre>
 * {@literal @}Component
 * {@literal @}Prototype
 * final class ShoppingCart {
 *   private val items = AtomicReference(List.empty[Item])
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface Prototype {}
