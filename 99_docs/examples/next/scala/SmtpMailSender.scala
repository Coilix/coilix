package com.example

import dev.coilix.annotation.Component
import dev.coilix.annotation.Profile
import dev.coilix.annotation.Value

@Component
@Profile(Array("!dev"))
final class SmtpMailSender(@Value("${mail.host}") host: String) extends MailSender {
  override def send(to: String, text: String): Unit = ()
}
