package dev.coilix.concurrency

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/**
 * Runs tasks on threads managed by Coilix, so beans do not create their own threads.
 *
 * The concurrency module registers a `taskExecutor` bean on virtual threads, closed with the context. An application can
 * register its own executor marked `@Primary`, which is then injected wherever a `TaskExecutor` is required:
 *
 * ```scala
 * @Service
 * final class ReportService(executor: TaskExecutor) {
 *   def render(id: ReportId): Future[Report] = executor.submit(renderer.render(id))
 * }
 * ```
 */
trait TaskExecutor {

  /** Runs `task` on another thread and does not wait for it. A failure of the task is logged. */
  def execute(task: => Unit): Unit

  /** Runs `task` on another thread and returns its result, or its failure, as a `Future`. */
  def submit[T](task: => T): Future[T]

  /** The executor as an `ExecutionContext`, for `Future` combinators and libraries that take one. */
  def executionContext: ExecutionContext
}
