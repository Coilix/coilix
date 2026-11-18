package com.example

trait MailSender {
  def send(to: String, text: String): Unit
}
