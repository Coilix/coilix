package dev.coilix.context

import dev.coilix.context.configured.Salutation
import dev.coilix.context.configured.SalutationConfiguration
import dev.coilix.context.configured.ServerSettings
import dev.coilix.context.configured.WordSalutation
import munit.FunSuite

final class InstanceFactoryTest extends FunSuite {

  test("creates an instance with a constructor") {
    val factory = ConstructorInstanceFactory(classOf[WordSalutation].getConstructors.head)
    assertEquals(classOf[Salutation].cast(factory.create(List("Hey"))).salute("Ann"), "Hey Ann")
  }

  test("creates an instance with a method of the owner") {
    val factory = MethodInstanceFactory(classOf[SalutationConfiguration].getMethod("salutation", classOf[String]))
    assertEquals(classOf[Salutation].cast(factory.create(List(SalutationConfiguration(), "Yo"))).salute("Ann"), "Yo Ann")
  }

  test("fails without an owner for a method") {
    val factory = MethodInstanceFactory(classOf[SalutationConfiguration].getMethod("salutation", classOf[String]))
    intercept[BeanCreationException](factory.create(Nil))
  }

  test("binds an instance from the environment") {
    val environment = Providers.fromHocon("server.host = h").environment
    assertEquals(PropertiesInstanceFactory(environment, "server", classOf[ServerSettings]).create(Nil), ServerSettings("h"))
  }
}
