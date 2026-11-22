package dev.coilix.annotation;

import java.lang.annotation.*;

/**
 * Creates a singleton bean only when it is first needed, instead of when the context starts.
 *
 * <p>The bean is created by the first {@code getBean}, the first {@code get()} of a {@code
 * Provider[T]}, or the first bean created later that requires it. A failure of its constructor or
 * {@code @PostConstruct} method therefore shows up at that moment, not at startup. Lazy beans that
 * were never created are not scanned for {@link EventListener} and {@link Scheduled} methods. Use
 * it on a component class or on a {@link Bean} method.
 *
 * <pre>
 * {@literal @}Service
 * {@literal @}Lazy
 * final class ReportArchive(storage: Storage) {
 *   def find(id: ReportId): Option[Report] = storage.read(id)
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface Lazy {}
