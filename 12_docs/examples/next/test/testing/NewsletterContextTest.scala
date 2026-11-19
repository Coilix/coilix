package com.example
package testing

import dev.coilix.testkit.CoilixSuite
import dev.coilix.testkit.CoilixTestSetup

final class NewsletterContextTest extends munit.FunSuite with CoilixSuite {
  private val newsletter = coilixSuiteContext(
    CoilixTestSetup
      .forApplication[NewsletterApplication.type]
      .withProperties("newsletter.smtp-host" -> "localhost", "newsletter.sender-name" -> "The Team")
  )

  override def munitFixtures = List(newsletter)

  newsletter.test("reads the sender name") {
    assertEquals(property[String]("newsletter.sender-name"), Some("The Team"))
  }

  newsletter.test("creates one welcome mail") {
    assertEquals(beans[WelcomeMail].size, 1)
  }
}
