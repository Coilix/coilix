package com.example
package tasks

import dev.coilix.annotation.Component
import dev.coilix.concurrency.RetryPolicy
import dev.coilix.concurrency.TaskScheduler
import scala.concurrent.duration.DurationInt
import scala.util.Try

@Component
class RetryingMailer(mailer: MailSender, scheduler: TaskScheduler) {
  private val retries = RetryPolicy(maxAttempts = 4, initialDelay = 1.second, maxDelay = 30.seconds, multiplier = 2.0, jitter = 0.2)

  def send(to: String, text: String): Unit = attempt(to, text, 1)

  private def attempt(to: String, text: String, attempts: Int): Unit =
    Try(mailer.send(to, text)).failed.foreach(_ => retryLater(to, text, attempts))

  private def retryLater(to: String, text: String, attempts: Int): Unit =
    retries.delayAfter(attempts).foreach(delay => scheduler.schedule(delay)(attempt(to, text, attempts + 1)))
}
