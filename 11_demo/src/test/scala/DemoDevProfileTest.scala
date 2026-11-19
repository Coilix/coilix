package dev.coilix.demo

import dev.coilix.demo.workshop.ConsoleNotifier
import dev.coilix.demo.workshop.Notifier
import dev.coilix.demo.workshop.WorkshopProperties
import dev.coilix.testkit.CoilixFixtures
import dev.coilix.testkit.CoilixTestSetup
import munit.FunSuite

final class DemoDevProfileTest extends FunSuite with CoilixFixtures {
  private val shared = coilixSuiteContext(CoilixTestSetup.forApplication[DemoApplication.type].withProfiles("dev"))

  override def munitFixtures = List(shared)

  private def context = shared()

  test("uses the dev notifier") {
    assertEquals(context.getBean[Notifier].getClass, classOf[ConsoleNotifier])
  }

  test("reads the workshop name from application-dev.yaml") {
    assertEquals(context.getBean[WorkshopProperties].name, "Northside Garage (dev)")
  }
}
