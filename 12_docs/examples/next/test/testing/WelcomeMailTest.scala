package com.example
package testing

import dev.coilix.testkit.CoilixSuite
import dev.coilix.testkit.CoilixTestSetup

final class WelcomeMailTest extends munit.FunSuite with CoilixSuite {
  private given newsletter: CoilixTestSetup = CoilixTestSetup
    .forApplication[NewsletterApplication.type]
    .withProperties("newsletter.smtp-host" -> "localhost", "newsletter.sender-name" -> "The Team")

  private val mailer = RecordingMailer()

  coilixTest("uses the SMTP mailer") {
    assertEquals(bean[Mailer].getClass, classOf[SmtpMailer])
  }

  coilixTest("sends a welcome mail through the replaced mailer", newsletter.replaceBean[Mailer](mailer)) {
    bean[WelcomeMail].sendTo("ann@example.com")
    assertEquals(mailer.sent, List("ann@example.com" -> "Welcome, ann@example.com! The Team"))
  }
}
