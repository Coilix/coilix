package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.context.modules.SampleModule
import dev.coilix.context.modules.SampleService
import munit.FunSuite

final class ServiceLoaderModuleLoaderTest extends FunSuite {

  test("loads the modules listed in META-INF/services") {
    given ComponentProvider = ComponentProvider.builtIn
    val modules             = ModuleLoader.default.load()
    assertEquals(modules.map(_.getClass), List(classOf[SampleModule]))
    assertEquals(modules.flatMap(_.components), List(classOf[SampleService]))
  }

  test("skips a module disabled in the configuration") {
    given ComponentProvider = Providers.fromHocon("coilix.modules.sample.enabled = false")
    assertEquals(ModuleLoader.default.load(), Nil)
  }

  test("keeps a module enabled explicitly in the configuration") {
    given ComponentProvider = Providers.fromHocon("coilix.modules.sample.enabled = true")
    assertEquals(ModuleLoader.default.load().map(_.name), List("sample"))
  }

  test("finds no modules through a class loader that does not list any") {
    val loader = ServiceLoaderModuleLoader(ClassLoader.getPlatformClassLoader, ComponentProvider.builtIn.environment)
    assertEquals(loader.load(), Nil)
  }
}
