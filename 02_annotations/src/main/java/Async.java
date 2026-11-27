package dev.coilix.annotation;

import java.lang.annotation.*;

/**
 * Marks a method of a bean that returns {@code IO} and runs on the compute pool of the concurrency
 * module, or on its blocking executor, instead of the calling fiber.
 *
 * <p>A call returns an {@code IO}, and the bean is called only when that {@code IO} runs: on the
 * compute pool by default, or on the blocking executor for a method that sets {@link #blocking()},
 * for example one that waits for a database or another service. A method returning {@code IO[A]}
 * gives its result to the caller. A method returning {@code IO[Unit]} is started on a fiber of its
 * own and the caller does not wait for it; its failure is logged, and the fiber is cancelled when
 * the context starts closing.
 *
 * <p>The method must be declared in a trait of the bean, because Coilix registers the bean as a
 * proxy of that trait: other beans inject the trait, not the class. A call from inside the bean
 * ({@code this.method()}) does not pass the proxy and runs on the calling fiber.
 *
 * <pre>
 * trait ReportService {
 *   def render(id: ReportId): IO[Report]
 * }
 *
 * {@literal @}Service
 * final class PdfReportService(renderer: PdfRenderer) extends ReportService {
 *   {@literal @}Async(blocking = true)
 *   override def render(id: ReportId): IO[Report] = IO.pure(renderer.render(id))
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface Async {

    /**
     * Whether the method waits, for example for I/O, and runs on the blocking executor instead of
     * the compute pool.
     *
     * @return {@code true} for a method that blocks, {@code false} by default
     */
    boolean blocking() default false;
}
