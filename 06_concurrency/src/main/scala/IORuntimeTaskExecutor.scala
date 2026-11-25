package dev.coilix.concurrency

import cats.effect.unsafe.IORuntime
import dev.coilix.concurrency.TaskExecutor
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future

/**
 * A [[dev.coilix.concurrency.TaskExecutor]] on the compute pool of a cats-effect runtime, so tasks of Coilix, `IO` fibers and,
 * fs2 streams share one pool with one thread per core. The `ioTaskExecutor` bean of the concurrency module.
 *
 * The context of `propagators`, such as the MDC, is captured when a task is submitted and restored around it. A failure of a
 * task started with `execute` is logged and a fatal error goes to `fatalErrors`. The executor does not shut the pool down: the
 * runtime owns it and closes it with the context.
 *
 * @param runtime
 *   the runtime whose compute pool runs the tasks
 * @param fatalErrors
 *   receives the fatal errors of tasks
 * @param propagators
 *   carry thread-bound context from the submitting thread to the task
 */
final class IORuntimeTaskExecutor(runtime: IORuntime, fatalErrors: FatalErrorHandler, propagators: List[ContextPropagator])
    extends TaskExecutor {

  private val name = "io-compute-*"

  override val executionContext: ExecutionContextExecutor = new ExecutionContextExecutor {
    override def execute(task: Runnable): Unit = propagated(task.run())

    override def reportFailure(failure: Throwable): Unit = runtime.compute.reportFailure(failure)
  }

  override def execute(task: => Unit): Unit =
    propagated(FatalErrors.guarded(name, fatalErrors)(task))

  override def submit[T](task: => T): Future[T] =
    Future(task)(using executionContext)

  private def propagated(task: => Unit): Unit = {
    val snapshot = ContextPropagator.captureAll(propagators)
    runtime.compute.execute(() => snapshot.runWith(task))
  }
}
