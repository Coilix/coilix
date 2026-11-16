package com.example

import dev.coilix.annotation.Component
import dev.coilix.annotation.Profile
import dev.coilix.annotation.Slf4s

@Component
@Profile(Array("dev"))
@Slf4s
final class ConsoleMailSender extends MailSender {
  override def send(to: String, text: String): Unit = log.info(s"Mail to $to: $text")
}
