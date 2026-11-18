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

  private val shop = ClasspathEnvironmentLoader("profiles/shop", ConfigFormat.byPrecedence, classLoader)

  test("loads the files of the profiles listed in the base file over the base files") {
    val environment = shop.load(Map.empty)
    assertEquals(environment.activeProfiles, List("dev"))
    assertEquals((environment.get[String]("app.name"), environment.get[Int]("app.port")), (Some("dev"), Some(2)))
    assertEquals(environment.get[String]("app.region"), Some("eu"))
  }

  test("a profile listed later wins, and the command line can choose the profiles") {
    val environment = shop.load(Map(Profiles.ActivePath -> "dev, local"))
    assertEquals(environment.activeProfiles, List("dev", "local"))
    assertEquals((environment.get[String]("app.name"), environment.get[Int]("app.port")), (Some("dev"), Some(3)))
  }

  test("command line options still beat profile files") {
    assertEquals(shop.load(Map("app.port" -> "9")).get[Int]("app.port"), Some(9))
  }

  test("a profile file cannot activate further profiles") {
    val environment = shop.load(Map(Profiles.ActivePath -> "test"))
    assertEquals(environment.get[String]("app.name"), Some("base"))
  }

  test("no profiles when none are listed") {
    assertEquals(loader(ConfigFormat.byPrecedence*).load(Map.empty).activeProfiles, Nil)
  }
}
