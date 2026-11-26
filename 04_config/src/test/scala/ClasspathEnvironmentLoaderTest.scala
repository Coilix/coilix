package dev.coilix.config

import dev.coilix.config.ConfigError.orThrow
import munit.FunSuite

final class ClasspathEnvironmentLoaderTest extends FunSuite {
  private val classLoader = getClass.getClassLoader

  private def loader(formats: ConfigFormat*): EnvironmentLoader =
    ClasspathEnvironmentLoader("formats/sample", formats.toList, classLoader)

  test("loads the same structure from each format") {
    val names = List(HoconConfigFormat(), YamlConfigFormat(), PropertiesConfigFormat()).map(format =>
      loader(format).load(Map.empty).orThrow.get[String]("app.name")
    )
    assertEquals(names, List(Some("conf"), Some("yaml"), Some("props")))
  }

  test("merges formats by precedence") {
    val environment = loader(ConfigFormat.byPrecedence*).load(Map.empty).orThrow
    assertEquals(environment.get[String]("app.name"), Some("props"))
    assertEquals(environment.get[Int]("app.port"), Some(2))
    assertEquals(environment.bind("app.servers", classOf[List[?]]), List("e", "f"))
    assertEquals((environment.get[Boolean]("app.only-conf"), environment.get[Boolean]("app.only-yaml")), (Some(true), Some(true)))
  }

  test("command line overrides beat every file") {
    assertEquals(loader(ConfigFormat.byPrecedence*).load(Map("app.name" -> "cli")).orThrow.get[String]("app.name"), Some("cli"))
  }

  test("system properties beat files") {
    System.setProperty("app.port", "9")
    try {
      assertEquals(loader(ConfigFormat.byPrecedence*).load(Map.empty).orThrow.get[Int]("app.port"), Some(9))
    } finally {
      val _ = System.clearProperty("app.port")
    }
  }

  test("loads nothing when files are missing") {
    assert(!ClasspathEnvironmentLoader("missing", ConfigFormat.byPrecedence, classLoader).load(Map.empty).orThrow.contains("app"))
  }

  private val shop = ClasspathEnvironmentLoader("profiles/shop", ConfigFormat.byPrecedence, classLoader)

  test("loads the files of the profiles listed in the base file over the base files") {
    val environment = shop.load(Map.empty).orThrow
    assertEquals(environment.activeProfiles, List("dev"))
    assertEquals((environment.get[String]("app.name"), environment.get[Int]("app.port")), (Some("dev"), Some(2)))
    assertEquals(environment.get[String]("app.region"), Some("eu"))
  }

  test("a profile listed later wins, and the command line can choose the profiles") {
    val environment = shop.load(Map(Profiles.ActivePath -> "dev, local")).orThrow
    assertEquals(environment.activeProfiles, List("dev", "local"))
    assertEquals((environment.get[String]("app.name"), environment.get[Int]("app.port")), (Some("dev"), Some(3)))
  }

  test("command line options still beat profile files") {
    assertEquals(shop.load(Map("app.port" -> "9")).orThrow.get[Int]("app.port"), Some(9))
  }

  test("a profile file cannot activate further profiles") {
    val environment = shop.load(Map(Profiles.ActivePath -> "test")).orThrow
    assertEquals(environment.get[String]("app.name"), Some("base"))
  }

  test("no profiles when none are listed") {
    assertEquals(loader(ConfigFormat.byPrecedence*).load(Map.empty).orThrow.activeProfiles, Nil)
  }

  private def broken(name: String, format: ConfigFormat): Either[ConfigError, Environment] =
    ClasspathEnvironmentLoader(s"broken/$name", List(format), classLoader).load(Map.empty)

  test("returns an error for a HOCON file that cannot be parsed") {
    broken("syntax", HoconConfigFormat()) match {
      case Left(ConfigError.Unparsable(source, _)) => assert(source.endsWith("broken/syntax.conf"), source)
      case other                                   => fail(s"Expected an unparsable file, got $other")
    }
  }

  test("returns an error for a YAML file that cannot be parsed") {
    broken("syntax", YamlConfigFormat()) match {
      case Left(ConfigError.Unparsable(source, _)) => assert(source.endsWith("broken/syntax.yaml"), source)
      case other                                   => fail(s"Expected an unparsable file, got $other")
    }
  }

  test("returns an error for a substitution that no source defines") {
    val error = broken("unresolved", HoconConfigFormat()).left.map(_.message)
    assert(error.left.exists(_.contains("missing.key")), error)
  }

  test("returns an error when the profiles are a section") {
    val error = broken("profiles", HoconConfigFormat())
    assert(error.left.exists(_.isInstanceOf[ConfigError.Unreadable]), error)
  }
}
