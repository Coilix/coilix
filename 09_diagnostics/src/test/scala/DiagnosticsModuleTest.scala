package dev.coilix.diagnostics

import com.typesafe.config.ConfigFactory
import dev.coilix.concurrency.ConcurrencyModule
import dev.coilix.config.ComponentProvider
import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import dev.coilix.context.ApplicationContextBuilder
import dev.coilix.context.BeanDefinitionFactory
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.context.RegisteredBean
import dev.coilix.events.EventsModule
import munit.FunSuite
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.*

final class DiagnosticsModuleTest extends FunSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  test("registers the health, the threads indicator, the built-in checks and the monitor") {
    withContext("") { context =>
      assertEquals(context.getBean[Health].indicators.map(_.name), List("threads"))
      assertEquals(context.getBean[Health].liveness, HealthStatus.Up)
      assertEquals(
        context.getBeansOfType[ThreadCheck].map(_.getClass.getSimpleName).sorted,
        List("DeadlockCheck", "LongBlockedCheck", "ThreadCountCheck", "ThreadGrowthCheck")
      )
      assert(context.getBean[FindingReporter].isInstanceOf[LoggingFindingReporter])
    }
  }

  test("reads the settings from coilix.diagnostics") {
    withContext("coilix.diagnostics { interval = 2s, threads { on-deadlock = report, blocked-after = 1m } }") { context =>
      val settings = context.getBean[DiagnosticsSettings]
      assertEquals(
        (settings.interval, settings.threads.onDeadlock, settings.threads.blockedAfter),
        (2.seconds, DeadlockAction.Report, 1.minute)
      )
    }
  }

  test("runs the coilix-diagnostics thread while the context is open") {
    val context = contextOf("coilix.diagnostics.interval = 50ms")
    val running =
      try {
        Thread.sleep(200)
        diagnosticsThreadAlive
      } finally {
        context.close()
      }
    Thread.sleep(200)
    assertEquals((running, diagnosticsThreadAlive), (true, false))
  }

  private def diagnosticsThreadAlive: Boolean =
    Thread.getAllStackTraces.keySet.asScala.exists(thread => thread.getName == "coilix-diagnostics" && thread.isAlive)

  private def withContext(config: String)(body: ConfigurableApplicationContext => Unit): Unit = {
    val context = contextOf(config)
    try {
      body(context)
    } finally {
      context.close()
    }
  }

  private def contextOf(config: String): ConfigurableApplicationContext = {
    val environment = Environment.of(
      TypesafeConfigTree(ConfigFactory.parseString(s"coilix.fatal-errors.action = log\n$config"))
    )
    ApplicationContextBuilder.default.build(
      (ConcurrencyModule().components ++ EventsModule().components ++ DiagnosticsModule().components)
        .flatMap(BeanDefinitionFactory.default.create),
      List(RegisteredBean.of[Environment]("environment", environment))
    )
  }
}
