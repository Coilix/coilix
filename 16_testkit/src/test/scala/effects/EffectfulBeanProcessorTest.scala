package dev.coilix.testkit
package effects

import dev.coilix.concurrency.EffectfulBeanProcessor
import dev.coilix.concurrency.InvalidEffectfulBeanException
import dev.coilix.config.ComponentProvider
import dev.coilix.context.BeanDefinition
import dev.coilix.context.BeanDefinitionFactory
import dev.coilix.testkit.effects.lazyeffect.SnapshotConfiguration
import dev.coilix.testkit.effects.lazywarmup.LazyWarmup
import dev.coilix.testkit.effects.shop.ShopResources
import dev.coilix.testkit.effects.warmup.PriceCache
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

  test("makes a bean whose lifecycle methods return IO wait for the runtime and the settings") {
    assertEquals(
      definitions(classOf[PriceCache]).map(processor.process).map(_.dependsOn),
      List(List("ioRuntime", "catsEffectSettings"))
    )
  }

  test("rejects a lazy bean whose lifecycle methods return IO") {
    val failure = intercept[InvalidEffectfulBeanException](definitions(classOf[LazyWarmup]).map(processor.process))
    assert(failure.getMessage.contains("'lazyWarmup' is made or initialized by an effect"), failure.getMessage)
  }

  private def definitions(beanClass: Class[?]): List[BeanDefinition] =
    BeanDefinitionFactory.default(using ComponentProvider.builtIn).create(beanClass)
}
