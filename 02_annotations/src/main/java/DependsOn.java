package dev.coilix.annotation;

import java.lang.annotation.*;

/**
 * Creates the bean only after the beans with the given names, although it does not inject them.
 *
 * <p>Use it when one bean relies on a side effect of another, for example a cache that reads tables
 * that database migrations create. The named beans are created first and destroyed after this bean.
 * A name that matches no bean, or that names a {@link Prototype} bean, stops the start of the
 * context. A bean that depends on itself, directly or through other beans, is a circular
 * dependency. Use it on a component class or on a {@link Bean} method.
 *
 * <pre>
 * {@literal @}Service
 * {@literal @}DependsOn(Array("schemaMigrations"))
 * final class PriceCache(prices: PriceRepository) {
 *   private val current = prices.findAll()
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface DependsOn {
    /**
     * The names of the beans to create before this one.
     *
     * @return the bean names
     */
    String[] value();
}
