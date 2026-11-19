package com.example
package testing

import dev.coilix.testkit.CoilixFixtures
import dev.coilix.testkit.CoilixTestSetup

final class WelcomeMailFixtureTest extends munit.FunSuite with CoilixFixtures {
  private val mailer  = RecordingMailer()
  private val context = coilixContext(
    CoilixTestSetup
      .forApplication[NewsletterApplication.type]
      .withProperties("newsletter.smtp-host" -> "localhost", "newsletter.sender-name" -> "The Team")
      .replaceBean[Mailer](mailer)
  )

  context.test("sends a welcome mail through the replaced mailer") { ctx =>
    ctx.getBean[WelcomeMail].sendTo("ann@example.com")
    assertEquals(mailer.sent.map(_._1), List("ann@example.com"))
  }
}
