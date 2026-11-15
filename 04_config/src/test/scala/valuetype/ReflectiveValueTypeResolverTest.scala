package dev.coilix.config
package valuetype

class ReflectiveValueTypeResolverTest extends munit.FunSuite {

  private val resolver = ValueTypeResolver.default

  test("recognizes only annotated classes") {
    assert(resolver.isValueType(classOf[Port]))
    assert(resolver.isValueType(classOf[Secret]))
    assert(!resolver.isValueType(classOf[Plain]))
  }

  test("describes the inner value of a value type") {
    val port = resolver.resolve(classOf[Port])
    assertEquals(port.valueType, classOf[Port])
    assertEquals(port.innerClass, classOf[Int])
    assertEquals(resolver.resolve(classOf[Tags]).innerType.getTypeName, "scala.collection.immutable.List<java.lang.String>")
  }

  test("creates a value type and reads its inner value back") {
    val port  = resolver.resolve(classOf[Port])
    val value = port.create(Int.box(8080))
    assertEquals(value, Port(8080))
    assertEquals(port.extract(value), Int.box(8080))
  }

  test("reports a value rejected by the constructor with the class, the value and the cause") {
    val error = intercept[ValueCreationException](resolver.resolve(classOf[Port]).create(Int.box(0)))
    assertEquals(
      error.getMessage,
      "Cannot create dev.coilix.config.valuetype.Port from '0': requirement failed: Port must be positive, but is 0"
    )
    assert(error.getCause.isInstanceOf[IllegalArgumentException])
  }

  test("reports a value of the wrong type") {
    val error = intercept[ValueCreationException](resolver.resolve(classOf[Port]).create("eighty"))
    assert(error.getMessage.startsWith("Cannot create dev.coilix.config.valuetype.Port from 'eighty'"), error.getMessage)
  }

  test("rejects a class without the annotation") {
    assertInvalid(classOf[Plain], "is not annotated with @ValueType")
  }

  test("rejects an abstract class") {
    assertInvalid(classOf[Shape], "must be a concrete class")
  }

  test("rejects a constructor with more than one parameter") {
    assertInvalid(classOf[Coordinates], "must have exactly one public constructor with one parameter")
  }

  test("rejects a class whose inner value cannot be read") {
    assertInvalid(classOf[Hidden], "must have a public field or method 'value' that returns the inner value")
  }

  test("a secret hides its text") {
    val secret = Secret("s3cr3t")
    assertEquals(secret.toString, "*****")
    assertEquals(s"key=$secret", "key=*****")
    assertEquals(secret.value, "s3cr3t")
  }

  private def assertInvalid(valueType: Class[?], problem: String)(using munit.Location): Unit = {
    val error = intercept[InvalidValueTypeException](resolver.resolve(valueType))
    assertEquals(error.getMessage, s"Value type ${valueType.getName} $problem")
  }
}
