package com.example
package tasks

import dev.coilix.annotation.Service
import dev.coilix.concurrency.TaskExecutor
import scala.concurrent.Future

@Service
final class TenantReport(executor: TaskExecutor) {
  def build(): Future[String] =
    executor.submit(s"report for ${CurrentTenant.get.getOrElse("nobody")}")
}
