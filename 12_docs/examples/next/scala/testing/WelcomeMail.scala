package com.example
package testing

import dev.coilix.annotation.Component
import dev.coilix.annotation.Value

@Component
final class WelcomeMail(mailer: Mailer, @Value("${newsletter.sender-name}") senderName: String) {
  def sendTo(user: String): Unit = mailer.send(user, s"Welcome, $user! $senderName")
}
