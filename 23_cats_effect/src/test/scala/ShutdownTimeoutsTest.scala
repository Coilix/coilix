package dev.coilix.catseffect

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.typesafe.config.ConfigFactory
import dev.coilix.catseffect.shop.ShopResources
import dev.coilix.catseffect.slow.DrainConfiguration
import dev.coilix.config.ComponentProvider
import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import dev.coilix.context.BeanDefinition
import dev.coilix.context.BeanDefinitionFactory
import munit.FunSuite
import scala.concurrent.duration.DurationInt

final class ShutdownTimeoutsTest extends FunSuite {
  private val timeouts = ShutdownTimeouts(CatsEffectSettings(shutdownTimeout = 7.seconds), None)

  test("uses the limit of the settings for a bean without @ShutdownTimeout") {
    assertEquals(timeouts.of(definition(classOf[ShopResources], "connections")), 7.seconds)
  }

  test("reads @ShutdownTimeout, with the default of its placeholder") {
    assertEquals(timeouts.of(definition(classOf[DrainConfiguration], "stuckDrain")), 100.millis)
  }

  test("resolves the placeholder of @ShutdownTimeout from the environment") {
    val configured = ShutdownTimeouts(
      CatsEffectSettings(),
      Some(Environment.of(TypesafeConfigTree(ConfigFactory.parseString("test.drain-timeout = 3s"))))
    )
    assertEquals(configured.of(definition(classOf[DrainConfiguration], "stuckDrain")), 3.seconds)
  }

  test("logs a failing finalizer instead of raising it") {
    timeouts.bounded(
      definition(classOf[ShopResources], "connections"),
      IO.raiseError(IllegalStateException("broken"))
    ).unsafeRunSync()
  }

  test("gives up on a finalizer that takes longer than its limit") {
    val started = System.nanoTime
    timeouts.bounded(definition(classOf[DrainConfiguration], "stuckDrain"), IO.never).unsafeRunSync()
    assert(System.nanoTime - started < 5.seconds.toNanos)
  }

  private def definition(beanClass: Class[?], beanName: String): BeanDefinition =
    BeanDefinitionFactory.default(using ComponentProvider.builtIn).create(beanClass).filter(_.beanName == beanName).head
}
