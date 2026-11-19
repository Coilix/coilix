package com.example
package testing

import dev.coilix.testkit.CoilixFixtures
import dev.coilix.testkit.CoilixTestSetup

final class NewsletterContextTest extends munit.FunSuite with CoilixFixtures {
  private val context = coilixSuiteContext(
    CoilixTestSetup
      .forApplication[NewsletterApplication.type]
      .withProperties("newsletter.smtp-host" -> "localhost", "newsletter.sender-name" -> "The Team")
  )

  override def munitFixtures = List(context)

  test("uses the SMTP mailer") {
    assertEquals(context().getBean[Mailer].getClass, classOf[SmtpMailer])
  }

  test("creates the welcome mail") {
    assertEquals(context().getBeansOfType[WelcomeMail].size, 1)
  }
}
