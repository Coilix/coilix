package dev.coilix.annotation;

import java.lang.annotation.*;

/**
 * Marks a class as a bean that Coilix creates during component scanning.
 *
 * <p>The bean is named after the class with the first letter in lower case ({@code OrderService}
 * becomes {@code orderService}). Its dependencies are injected through the constructor chosen as
 * described in {@link Autowired}.
 *
 * <pre>
 * {@literal @}Component
 * final class PriceCalculator(taxRates: TaxRates)</pre>
 *
 * @see Service
 * @see Repository
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface Component {}
