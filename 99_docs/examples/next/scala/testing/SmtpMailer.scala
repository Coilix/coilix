package com.example
package testing

import dev.coilix.annotation.Component
import dev.coilix.annotation.Value

@Component
final class SmtpMailer(@Value("${newsletter.smtp-host}") host: String) extends Mailer {
  override def send(to: String, text: String): Unit = ()
}
