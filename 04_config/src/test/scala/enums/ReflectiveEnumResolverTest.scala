package dev.coilix.config
package enums

class ReflectiveEnumResolverTest extends munit.FunSuite {

  private val resolver = EnumResolver.default

  test("recognizes only annotated enums") {
    assert(resolver.isEnum(classOf[Mode]))
    assert(!resolver.isEnum(classOf[Level]))
  }

  test("lists the cases in the order of declaration with their names") {
    val modes = resolver.resolve(classOf[Mode])
    assertEquals(modes.enumType, classOf[Mode])
    assertEquals(modes.cases.map(_.value), List(Mode.Fast, Mode.Safe, Mode.ReadOnly))
    assertEquals(modes.names, List("Fast", "Safe", "read-only"))
    assert(!modes.ignoreCase)
  }

  test("finds a case by its name") {
    val modes = resolver.resolve(classOf[Mode])
    assertEquals(modes.find("Fast"), Some(Mode.Fast))
    assertEquals(modes.find("read-only"), Some(Mode.ReadOnly))
    assertEquals(modes.find("ReadOnly"), None)
    assertEquals(modes.find("fast"), None)
  }

  test("finds a case by an alias and ignores letter case when the enum asks for it") {
    val plans = resolver.resolve(classOf[Plan])
    assertEquals(plans.cases.map(_.allNames), List(List("basic", "free"), List("Premium")))
    assertEquals(plans.find("FREE"), Some(Plan.Basic))
    assertEquals(plans.find("premium"), Some(Plan.Premium))
  }

  test("reads enums whose cases pass constructor arguments") {
    assertEquals(resolver.resolve(classOf[Planet]).names, List("Earth", "Mars"))
  }

  test("valueOf reports an unknown name with the allowed names") {
    val error = intercept[UnknownEnumValueException](resolver.resolve(classOf[Mode]).valueOf("slow"))
    assertEquals(error.getMessage, "Unknown dev.coilix.config.enums.Mode 'slow', expected one of: Fast, Safe, read-only")
    assertEquals(resolver.resolve(classOf[Mode]).valueOf("Safe"), Mode.Safe)
  }

  test("nameOf returns the name of a case and rejects other values") {
    val modes = resolver.resolve(classOf[Mode])
    assertEquals(modes.nameOf(Mode.ReadOnly), "read-only")
    val error = intercept[IllegalArgumentException](modes.nameOf(Plan.Basic))
    assertEquals(error.getMessage, "Basic is not a case of dev.coilix.config.enums.Mode")
  }

  test("rejects a class without the annotation") {
    assertInvalid(classOf[Level], "is not annotated with @Enumerated")
  }

  test("rejects an annotated class that is not an enum") {
    assertInvalid(classOf[Settings], "must be a Scala 3 enum")
  }

  test("rejects an enum with cases that have parameters") {
    assertInvalid(classOf[Shape], "must have only cases without parameters")
  }

  test("rejects cases that share a name or an alias") {
    assertInvalid(classOf[Clash], "has several cases named b")
  }

  private def assertInvalid(enumType: Class[?], problem: String)(using munit.Location): Unit = {
    val error = intercept[InvalidEnumException](resolver.resolve(enumType))
    assertEquals(error.getMessage, s"Enum ${enumType.getName} $problem")
  }
}
