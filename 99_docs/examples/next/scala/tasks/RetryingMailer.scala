package com.example
package tasks

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.concurrency.RetryPolicy
import scala.concurrent.duration.DurationInt

@Component
class RetryingMailer(mailer: MailSender) {
  private val retries = RetryPolicy(maxAttempts = 4, initialDelay = 1.second, maxDelay = 30.seconds, multiplier = 2.0, jitter = 0.2)

  def send(to: String, text: String): IO[Unit] = attempt(to, text, 1)

  private def attempt(to: String, text: String, attempts: Int): IO[Unit] =
    IO.blocking(mailer.send(to, text)).handleErrorWith(failure =>
      retries.delayAfter(attempts).fold(IO.raiseError(failure))(delay => IO.sleep(delay) *> attempt(to, text, attempts + 1))
    )
}
