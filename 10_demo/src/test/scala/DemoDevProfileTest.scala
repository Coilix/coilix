package dev.coilix.demo

import dev.coilix.boot.Coilix
import munit.FunSuite

final class DemoDevProfileTest extends FunSuite {
  private val context = Coilix.run[DemoApplication.type](Array("--coilix.profiles.active=dev"))

  override def afterAll(): Unit = context.close()

  test("uses the dev notifier") {
    assertEquals(context.getBean[Notifier].getClass, classOf[ConsoleNotifier])
  }

  test("reads the greeting from application-dev.yaml") {
    assertEquals(context.getBean[Greeter].greet("Ann"), "Hello from dev, Ann")
  }
}
