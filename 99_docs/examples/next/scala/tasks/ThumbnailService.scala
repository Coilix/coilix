package com.example
package tasks

import dev.coilix.annotation.Service
import dev.coilix.concurrency.TaskExecutor
import scala.concurrent.Future

@Service
final class ThumbnailService(executor: TaskExecutor) {
  def render(image: String): Future[String] =
    executor.submit(s"thumbnail of $image rendered on ${Thread.currentThread.getName}")
}
