package dev.coilix.config

import munit.FunSuite

final class ClasspathEnvironmentLoaderTest extends FunSuite {
  private val classLoader = getClass.getClassLoader

  private def loader(formats: ConfigFormat*): EnvironmentLoader =
    ClasspathEnvironmentLoader("formats/sample", formats.toList, classLoader)

  test("loads the same structure from each format") {
    val names = List(HoconConfigFormat(), YamlConfigFormat(), PropertiesConfigFormat()).map(format =>
      loader(format).load(Map.empty).get[String]("app.name")
    )
    assertEquals(names, List(Some("conf"), Some("yaml"), Some("props")))
  }

  test("merges formats by precedence") {
    val environment = loader(ConfigFormat.byPrecedence*).load(Map.empty)
    assertEquals(environment.get[String]("app.name"), Some("props"))
    assertEquals(environment.get[Int]("app.port"), Some(2))
    assertEquals(environment.bind("app.servers", classOf[List[?]]), List("e", "f"))
    assertEquals((environment.get[Boolean]("app.only-conf"), environment.get[Boolean]("app.only-yaml")), (Some(true), Some(true)))
  }

  test("command line overrides beat every file") {
    assertEquals(loader(ConfigFormat.byPrecedence*).load(Map("app.name" -> "cli")).get[String]("app.name"), Some("cli"))
  }

  test("system properties beat files") {
    System.setProperty("app.port", "9")
    try {
      assertEquals(loader(ConfigFormat.byPrecedence*).load(Map.empty).get[Int]("app.port"), Some(9))
    } finally {
      val _ = System.clearProperty("app.port")
    }
  }

  test("loads nothing when files are missing") {
    assert(!ClasspathEnvironmentLoader("missing", ConfigFormat.byPrecedence, classLoader).load(Map.empty).contains("app"))
  }
}
