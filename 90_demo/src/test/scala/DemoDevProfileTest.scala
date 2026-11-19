package dev.coilix.demo

import dev.coilix.demo.workshop.ConsoleNotifier
import dev.coilix.demo.workshop.Notifier
import dev.coilix.demo.workshop.WorkshopProperties
import dev.coilix.testkit.CoilixSuite
import dev.coilix.testkit.CoilixTestSetup
import munit.FunSuite

final class DemoDevProfileTest extends FunSuite with CoilixSuite {
  private val dev = coilixSuiteContext(CoilixTestSetup.forApplication[DemoApplication.type].withProfiles("dev"))

  override def munitFixtures = List(dev)

  dev.test("uses the dev notifier") {
    assertEquals(bean[Notifier].getClass, classOf[ConsoleNotifier])
  }

  dev.test("reads the workshop name from application-dev.yaml") {
    assertEquals(bean[WorkshopProperties].name, "Northside Garage (dev)")
  }
}
