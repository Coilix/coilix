package dev.coilix.concurrency
package custom

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

final class CallerRunsExecutor extends TaskExecutor {
  override def execute(task: => Unit): Unit = task

  override def submit[T](task: => T): Future[T] = Future.fromTry(scala.util.Try(task))

  override def executionContext: ExecutionContext = ExecutionContext.parasitic
}
