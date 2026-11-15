package dev.coilix.annotation;

import java.lang.annotation.*;

/**
 * Marks a method that runs when the application context closes, for example to release a connection
 * or stop a thread pool.
 *
 * <p>Beans are destroyed in the reverse order of their creation, so a bean is destroyed before the
 * beans it depends on. The method takes no parameters, returns {@code Unit} and may be private. An
 * exception thrown by the method is logged and does not stop the other beans from being destroyed.
 *
 * <pre>
 * {@literal @}Component
 * final class ConnectionPool {
 *   {@literal @}PreDestroy
 *   def shutdown(): Unit = connections.foreach(_.close())
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface PreDestroy {}
