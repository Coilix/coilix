package dev.coilix.annotation;

import java.lang.annotation.*;

/**
 * Marks a method of a bean that runs on the task executor of the concurrency module instead of the
 * calling thread.
 *
 * <p>A method returning {@code Future[T]} gives the result of the run on the executor. A method
 * returning {@code Unit} is started and not waited for, and its failure is logged. The method must
 * be declared in a trait of the bean, because Coilix registers the bean as a proxy of that trait:
 * other beans inject the trait, not the class. A call from inside the bean ({@code this.method()})
 * does not pass the proxy and runs on the calling thread.
 *
 * <pre>
 * trait ReportService {
 *   def render(id: ReportId): Future[Report]
 * }
 *
 * {@literal @}Service
 * final class PdfReportService(renderer: PdfRenderer) extends ReportService {
 *   {@literal @}Async
 *   override def render(id: ReportId): Future[Report] = Future.successful(renderer.render(id))
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface Async {}
