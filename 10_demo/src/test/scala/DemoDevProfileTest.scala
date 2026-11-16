package dev.coilix.demo

import dev.coilix.boot.Coilix
import dev.coilix.demo.workshop.ConsoleNotifier
import dev.coilix.demo.workshop.Notifier
import dev.coilix.demo.workshop.WorkshopProperties
import munit.FunSuite

final class DemoDevProfileTest extends FunSuite {
  private val context = Coilix.run[DemoApplication.type](Array("--coilix.profiles.active=dev"))

  override def afterAll(): Unit = context.close()

  test("uses the dev notifier") {
    assertEquals(context.getBean[Notifier].getClass, classOf[ConsoleNotifier])
  }

  test("reads the workshop name from application-dev.yaml") {
    assertEquals(context.getBean[WorkshopProperties].name, "Northside Garage (dev)")
  }
}
