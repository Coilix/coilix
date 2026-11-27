package com.example
package tasks

import cats.effect.IO

trait InvoiceMailer {
  def send(invoice: String): IO[String]

  def remind(invoice: String): IO[Unit]
}
