package dev.coilix.catseffect

import dev.coilix.catseffect.lazyeffect.SnapshotConfiguration
import dev.coilix.catseffect.shop.ShopResources
import dev.coilix.config.ComponentProvider
import dev.coilix.context.BeanDefinition
import dev.coilix.context.BeanDefinitionFactory
import munit.FunSuite

final class EffectfulBeanProcessorTest extends FunSuite {
  private val processor = EffectfulBeanProcessor()

  test("makes a bean made by an effect wait for the runtime and the settings") {
    val processed = definitions(classOf[ShopResources]).map(processor.process).map(d => d.beanName -> d.dependsOn).toMap
    assertEquals(
      (processed("greeting"), processed("connections"), processed("shopResources")),
      (List("ioRuntime", "catsEffectSettings"), List("ioRuntime", "catsEffectSettings"), Nil)
    )
  }

  test("keeps the names the bean already depends on, without repeating them") {
    val greeting =
      definitions(classOf[ShopResources]).find(_.beanName == "greeting").map(_.copy(dependsOn = List("ioRuntime", "a")))
    assertEquals(greeting.map(processor.process).map(_.dependsOn), Some(List("ioRuntime", "a", "catsEffectSettings")))
  }

  test("rejects a lazy bean made by an effect") {
    val snapshot = definitions(classOf[SnapshotConfiguration]).filter(_.beanName == "snapshot")
    intercept[InvalidEffectfulBeanException](snapshot.map(processor.process))
  }

  private def definitions(beanClass: Class[?]): List[BeanDefinition] =
    BeanDefinitionFactory.default(using ComponentProvider.builtIn).create(beanClass)
}
