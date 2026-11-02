package io.github.coilix.annotation;

import java.lang.annotation.*;

/**
 * Marks the entry point of a Coilix application.
 *
 * <p>The annotated class is passed to {@code Coilix.run}, which scans its packages for components,
 * builds the application context and runs the {@code ApplicationRunner} beans.
 *
 * <pre>
 * {@literal @}CoilixApplication
 * object ShopApplication {
 *   def main(args: Array[String]): Unit = {
 *     val _ = Coilix.run[ShopApplication.type](args)
 *   }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface CoilixApplication {
    /**
     * Packages scanned for components, including their subpackages. When empty, Coilix scans the
     * package of the annotated class.
     *
     * @return fully qualified package names to scan
     */
    String[] scanBasePackages() default {};
}
