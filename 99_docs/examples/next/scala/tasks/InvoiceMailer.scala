package com.example
package tasks

import scala.concurrent.Future

trait InvoiceMailer {
  def send(invoice: String): Future[String]
}
