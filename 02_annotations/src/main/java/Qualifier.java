package dev.coilix.annotation;

import java.lang.annotation.*;

/**
 * Selects a bean by name for a constructor or {@code @Bean} method parameter, when several beans
 * match its type.
 *
 * <p>The bean with this name must exist and have the type of the parameter. The annotation works on
 * a parameter of a single bean, not on {@code Option[T]} or collection parameters. A bean name is
 * its {@code @Bean} method name or {@code @Bean} value, or the class name starting with a lower
 * case letter.
 *
 * <pre>
 * {@literal @}Component
 * final class Checkout({@literal @}Qualifier("cardPayments") payments: PaymentGateway)</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@Documented
public @interface Qualifier {
    /**
     * The name of the bean to inject.
     *
     * @return the bean name
     */
    String value();
}
