package org.scaling.context

import munit.FunSuite
import org.scaling.context.configured.Salutation
import org.scaling.context.configured.SalutationConfiguration
import org.scaling.context.configured.ServerSettings
import org.scaling.context.configured.WordSalutation

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
