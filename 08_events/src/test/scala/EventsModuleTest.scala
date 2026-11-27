package dev.coilix.events

import dev.coilix.concurrency.ConcurrencyModule
import dev.coilix.config.ComponentProvider
import dev.coilix.config.Environment
import dev.coilix.context.ApplicationContextBuilder
import dev.coilix.context.BeanDefinitionFactory
import dev.coilix.context.ServiceLoaderModuleLoader
import munit.FunSuite

final class EventsModuleTest extends FunSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  test("is found on the classpath") {
    assert(ServiceLoaderModuleLoader(getClass.getClassLoader, Environment.empty).load().exists(_.name == "events"))
  }

  test("registers one bus as the publisher, with the default settings in a context without an environment") {
    val context = ApplicationContextBuilder.default.build(
      (ConcurrencyModule().components ++ EventsModule().components).flatMap(BeanDefinitionFactory.default.create),
      Nil,
      ConcurrencyModule().effectInterpreter
    )
    try {
      assertEquals(
        (context.getBean[EventPublisher] eq context.getBean[EventBus], context.getBean[EventsSettings]),
        (true, EventsSettings())
      )
    } finally {
      context.close()
    }
  }
}
