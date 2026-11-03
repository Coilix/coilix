package dev.coilix.config

import dev.coilix.config.fixture.Greeting
import dev.coilix.config.fixture.Limits
import dev.coilix.config.fixture.MailSettings
import dev.coilix.config.fixture.PlainGreeting
import munit.FunSuite
import scala.concurrent.duration.DurationInt

final class ReflectiveConfigBinderTest extends FunSuite {
  private val binder = ConfigBinder.default

  private val mailTree = ConfigTrees.parse(
    """mail {
      |  host = smtp.example.com
      |  port = 2525
      |  recipients = [ann, bob]
      |  timeout = 5s
      |  reply-to = office
      |}""".stripMargin
  )

  test("binds a case class with defaults and kebab-case names") {
    assertEquals(
      binder.bind(mailTree, "mail", classOf[MailSettings]),
      MailSettings("smtp.example.com", 2525, List("ann", "bob"), 5.seconds, useTls = false, replyTo = Some("office"))
    )
  }

  test("binds a missing option parameter to none") {
    val tree = ConfigTrees.parse("mail { host = h, port = 1, recipients = [], timeout = 1s, use-tls = true }")
    assertEquals(
      binder.bind(tree, "mail", classOf[MailSettings]),
      MailSettings("h", 1, Nil, 1.second, useTls = true, replyTo = None)
    )
  }

  test("binds maps, sets and vectors") {
    val tree = ConfigTrees.parse("limits { max-size = 10, ratios { a = 0.5 }, tags = [x, x, y], levels = [1, 2] }")
    assertEquals(binder.bind(tree, "limits", classOf[Limits]), Limits(10L, Map("a" -> 0.5), Set("x", "y"), Vector(1, 2)))
  }

  test("binds properties-style lists") {
    val tree = ConfigTrees.parse("mail { host = h, port = 1, recipients { 0 = ann, 1 = bob }, timeout = 1s }")
    assertEquals(classOf[MailSettings].cast(binder.bind(tree, "mail", classOf[MailSettings])).recipients, List("ann", "bob"))
  }

  test("creates the class named under the class key") {
    val tree = ConfigTrees.parse("greeting.class = dev.coilix.config.fixture.PlainGreeting")
    assertEquals(binder.bind(tree, "greeting", classOf[Greeting]).getClass, classOf[PlainGreeting])
  }

  test("binds constructor parameters of the configured class recursively") {
    val tree = ConfigTrees.parse(
      """greeting {
        |  class = dev.coilix.config.fixture.DecoratedGreeting
        |  suffix = "!"
        |  service {
        |    class = dev.coilix.config.fixture.PrefixGreeting
        |    prefix = Hello
        |  }
        |}""".stripMargin
    )
    assertEquals(classOf[Greeting].cast(binder.bind(tree, "greeting", classOf[Greeting])).greet("Ann"), "Hello Ann!")
  }

  test("fails when the configured class does not exist") {
    val error = intercept[ConfigurationException](binder.bind(
      ConfigTrees.parse("greeting.class = com.example.Missing"),
      "greeting",
      classOf[Greeting]
    ))
    assert(error.getMessage.contains("com.example.Missing"))
  }

  test("fails when the configured class has the wrong type") {
    intercept[ConfigurationException](binder.bind(
      ConfigTrees.parse("greeting.class = java.lang.String"),
      "greeting",
      classOf[Greeting]
    ))
  }

  test("fails when a constructor parameter is not configured") {
    val tree  = ConfigTrees.parse("greeting.class = dev.coilix.config.fixture.DecoratedGreeting")
    val error = intercept[ConfigurationException](binder.bind(tree, "greeting", classOf[Greeting]))
    assert(error.getMessage.contains("greeting.service"))
  }

  test("fails for an abstract type without a class key") {
    intercept[ConfigurationException](binder.bind(ConfigTrees.parse("greeting.prefix = x"), "greeting", classOf[Greeting]))
  }

  test("reports a failing constructor") {
    val tree = ConfigTrees.parse("greeting.class = dev.coilix.config.fixture.FailingGreeting")
    intercept[ConfigurationException](binder.bind(tree, "greeting", classOf[Greeting]))
  }

  test("fails when a required value is missing") {
    intercept[ConfigurationException](binder.bind(ConfigTree.empty, "mail", classOf[MailSettings]))
  }
}
