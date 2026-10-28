package org.scaling.demo

import munit.FunSuite
import org.scaling.boot.ApplicationArguments
import org.scaling.boot.Scaling
import scala.concurrent.duration.DurationInt

final class DemoApplicationTest extends FunSuite {
  private val context = Scaling.run[DemoApplication.type](Array("--name=Ann", "--demo.greeting=Hey"))

  override def afterAll(): Unit = context.close()

  test("wires the primary user service") {
    assertEquals(context.getBean[UserService].getClass, classOf[SortedUserService])
  }

  test("registers every demo component") {
    assertEquals(
      context.beanNames.sorted,
      List(
        "applicationArguments",
        "applicationContext",
        "contextInspector",
        "defaultUserService",
        "demoConfiguration",
        "demoRunner",
        "environment",
        "greeter",
        "inMemoryUserRepository",
        "mailSettings",
        "sortedUserService"
      )
    )
  }

  test("exposes the command line arguments") {
    assertEquals(context.getBean[ApplicationArguments].optionValue("name"), Some("Ann"))
  }

  test("binds mail settings from application.yaml") {
    assertEquals(
      context.getBean[MailSettings],
      MailSettings("smtp.example.com", 2525, List("alice@example.com", "bob@example.com"), 5.seconds)
    )
  }

  test("command line options override application.yaml") {
    assertEquals(context.getBean[Greeter].greet("Ann"), "Hey, Ann")
  }
}
