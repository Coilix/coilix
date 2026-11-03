package dev.coilix.annotation;

import java.lang.annotation.*;

/**
 * Binds a section of the application configuration to a bean.
 *
 * <p>Instead of injecting other beans, Coilix creates the class from the configuration under the
 * given prefix. Constructor parameters are matched by name, both as written and in kebab case
 * ({@code connectTimeout} also matches {@code connect-timeout}). Parameters of type {@code Option}
 * may be missing, and Scala default arguments are used when a key is absent.
 *
 * <pre>
 * {@literal @}ConfigurationProperties("shop.mail")
 * final case class MailSettings(host: String, port: Int, timeout: FiniteDuration)</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface ConfigurationProperties {
    /**
     * The configuration prefix, for example {@code shop.mail}.
     *
     * @return the path of the configuration section bound to the bean
     */
    String value();
}
