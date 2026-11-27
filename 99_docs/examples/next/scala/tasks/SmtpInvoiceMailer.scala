package com.example
package tasks

import cats.effect.IO
import dev.coilix.annotation.Async
import dev.coilix.annotation.Service
import java.util.concurrent.LinkedBlockingQueue

@Service
final class SmtpInvoiceMailer extends InvoiceMailer {
  val reminders: LinkedBlockingQueue[String] = LinkedBlockingQueue[String]()

  @Async(blocking = true)
  override def send(invoice: String): IO[String] =
    IO.pure(s"$invoice sent from ${Thread.currentThread.getName}")

  @Async
  override def remind(invoice: String): IO[Unit] =
    IO(reminders.add(s"reminder for $invoice")).void
}
