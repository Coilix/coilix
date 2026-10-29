package io.github.coilix.demo

import io.github.coilix.boot.ApplicationArguments
import io.github.coilix.boot.Coilix
import munit.FunSuite
import scala.concurrent.duration.DurationInt

final class DemoApplicationTest extends FunSuite {
  private val context = Coilix.run[DemoApplication.type](Array("--name=Ann", "--demo.greeting=Hey"))

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
