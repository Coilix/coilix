package dev.coilix.config

import dev.coilix.config.ConfigError.orThrow
import dev.coilix.config.fixture.Greeting
import dev.coilix.config.fixture.Limits
import dev.coilix.config.fixture.MailSettings
import dev.coilix.config.fixture.PlainGreeting
import munit.FunSuite
import scala.concurrent.duration.DurationInt

final class ReflectiveConfigBinderTest extends FunSuite {
  private val binder = ConfigBinder.default

  private def bound(tree: ConfigTree, path: String, targetType: java.lang.reflect.Type): AnyRef =
    binder.bind(tree, path, targetType).orThrow

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
      bound(mailTree, "mail", classOf[MailSettings]),
      MailSettings("smtp.example.com", 2525, List("ann", "bob"), 5.seconds, useTls = false, replyTo = Some("office"))
    )
  }

  test("binds a missing option parameter to none") {
    val tree = ConfigTrees.parse("mail { host = h, port = 1, recipients = [], timeout = 1s, use-tls = true }")
    assertEquals(
      bound(tree, "mail", classOf[MailSettings]),
      MailSettings("h", 1, Nil, 1.second, useTls = true, replyTo = None)
    )
  }

  test("binds maps, sets and vectors") {
    val tree = ConfigTrees.parse("limits { max-size = 10, ratios { a = 0.5 }, tags = [x, x, y], levels = [1, 2] }")
    assertEquals(bound(tree, "limits", classOf[Limits]), Limits(10L, Map("a" -> 0.5), Set("x", "y"), Vector(1, 2)))
  }

  test("binds properties-style lists") {
    val tree = ConfigTrees.parse("mail { host = h, port = 1, recipients { 0 = ann, 1 = bob }, timeout = 1s }")
    assertEquals(classOf[MailSettings].cast(bound(tree, "mail", classOf[MailSettings])).recipients, List("ann", "bob"))
  }

  test("creates the class named under the class key") {
    val tree = ConfigTrees.parse("greeting.class = dev.coilix.config.fixture.PlainGreeting")
    assertEquals(bound(tree, "greeting", classOf[Greeting]).getClass, classOf[PlainGreeting])
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
    assertEquals(classOf[Greeting].cast(bound(tree, "greeting", classOf[Greeting])).greet("Ann"), "Hello Ann!")
  }

  test("returns an error when the configured class does not exist") {
    assertEquals(
      binder.bind(ConfigTrees.parse("greeting.class = com.example.Missing"), "greeting", classOf[Greeting]),
      Left(ConfigError.UnknownClass("greeting", "com.example.Missing"))
    )
  }

  test("returns an error when the configured class has the wrong type") {
    assertEquals(
      binder.bind(ConfigTrees.parse("greeting.class = java.lang.String"), "greeting", classOf[Greeting]),
      Left(ConfigError.IncompatibleClass("greeting", classOf[String], classOf[Greeting]))
    )
  }

  test("returns an error when a constructor parameter is not configured") {
    val tree   = ConfigTrees.parse("greeting.class = dev.coilix.config.fixture.DecoratedGreeting")
    val result = binder.bind(tree, "greeting", classOf[Greeting]).left.map(_.message)
    assert(result.left.exists(_.contains("greeting.service")), result)
  }

  test("returns an error for an abstract type without a class key") {
    assertEquals(
      binder.bind(ConfigTrees.parse("greeting.prefix = x"), "greeting", classOf[Greeting]),
      Left(ConfigError.AbstractTarget("greeting", classOf[Greeting]))
    )
  }

  test("returns a failing constructor with its cause") {
    val tree = ConfigTrees.parse("greeting.class = dev.coilix.config.fixture.FailingGreeting")
    binder.bind(tree, "greeting", classOf[Greeting]) match {
      case Left(ConfigError.CreationFailed("greeting", target, _)) => assertEquals(target.getSimpleName, "FailingGreeting")
      case other                                                   => fail(s"Expected a failed creation, got $other")
    }
  }

  test("returns an error when a required value is missing") {
    assertEquals(
      binder.bind(ConfigTree.empty, "mail", classOf[MailSettings]),
      Left(ConfigError.Missing("mail", classOf[MailSettings]))
    )
  }

  test("throws the error as a configuration exception with the same message") {
    val error = intercept[ConfigurationException](bound(ConfigTree.empty, "mail", classOf[MailSettings]))
    assertEquals(error.getMessage, s"Missing configuration 'mail' (expected ${classOf[MailSettings].getName})")
  }
}
