package com.example
package tasks

import dev.coilix.annotation.Async
import dev.coilix.annotation.Service
import scala.concurrent.Future

@Service
final class SmtpInvoiceMailer extends InvoiceMailer {
  @Async(blocking = true)
  override def send(invoice: String): Future[String] =
    Future.successful(s"$invoice sent from ${Thread.currentThread.getName}")
}
