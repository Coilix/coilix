package dev.coilix.concurrency

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/**
 * Runs tasks on threads managed by Coilix. The two kinds of executor differ in the work they are meant for:
 * [[dev.coilix.concurrency.TaskExecutor]] for computation and [[dev.coilix.concurrency.BlockingExecutor]] for calls that wait,
 * such as JDBC, HTTP or file access.
 */
trait ManagedExecutor {

  /** Runs `task` on another thread and does not wait for it. A failure of the task is logged. */
  def execute(task: => Unit): Unit

  /** Runs `task` on another thread and returns its result, or its failure, as a `Future`. */
  def submit[T](task: => T): Future[T]

  /** The executor as an `ExecutionContext`, for `Future` combinators and libraries that take one. */
  def executionContext: ExecutionContext
}
