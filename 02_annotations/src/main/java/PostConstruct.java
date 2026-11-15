package dev.coilix.annotation;

import java.lang.annotation.*;

/**
 * Marks a method that runs once after the bean is created and its dependencies are injected.
 *
 * <p>The method runs before any other bean receives this bean, so dependent beans always see it
 * initialized. It takes no parameters, returns {@code Unit} and may be private. A bean with several
 * such methods runs them in order of their names. An exception thrown by the method stops startup
 * with a {@code BeanCreationException} that names the bean.
 *
 * <pre>
 * {@literal @}Component
 * final class TemplateCache(loader: TemplateLoader) {
 *   {@literal @}PostConstruct
 *   private def warmUp(): Unit = loader.loadAll()
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface PostConstruct {}
