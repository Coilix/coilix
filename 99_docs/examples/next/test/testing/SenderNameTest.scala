package com.example
package testing

import dev.coilix.testkit.CoilixContextOps
import dev.coilix.testkit.CoilixTestSetup

final class SenderNameTest extends munit.FunSuite with CoilixContextOps {
  private val newsletter = CoilixTestSetup
    .forApplication[NewsletterApplication.type]
    .withProperties("newsletter.smtp-host" -> "localhost", "newsletter.sender-name" -> "The Team")

  test("reads the sender name inside a block") {
    val senderName = newsletter.use(property[String]("newsletter.sender-name"))
    assertEquals(senderName, Some("The Team"))
  }
}
