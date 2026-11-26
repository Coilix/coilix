package dev.coilix.logging

import cats.effect.IO
import dev.coilix.testkit.CoilixContextOps
import dev.coilix.testkit.CoilixIOTestSetup.*
import dev.coilix.testkit.CoilixTestSetup
import munit.CatsEffectSuite
import org.slf4j.LoggerFactory

final class LoggingModuleTest extends CatsEffectSuite with CoilixContextOps {

  private val recording = RecordingAppender()

  override def beforeAll(): Unit = {
    LoggerFactory.getILoggerFactory: Unit
    CoilixLogging.factory.appender.set(recording)
  }

  override def beforeEach(context: BeforeEach): Unit = {
    recording.clear()
    CoilixLogging.configure(LoggingSettings())
  }

  test("SLF4J uses the loggers of the module") {
    assert(CoilixLogging.isActive)
  }

  test("java.util.logging, Log4j 2 and Commons Logging write through the same loggers") {
    java.util.logging.Logger.getLogger("com.example.jul.Client").info("from jul")
    org.apache.logging.log4j.LogManager.getLogger("com.example.log4j.Client").info("from log4j")
    org.apache.commons.logging.LogFactory.getLog("com.example.jcl.Client").info("from jcl")
    java.util.logging.Logger.getLogger("com.example.jul.Client").fine("dropped at info")
    assertEquals(
      recording.messages,
      Vector(
        "INFO com.example.jul.Client from jul",
        "INFO com.example.log4j.Client from log4j",
        "INFO com.example.jcl.Client from jcl"
      )
    )
  }

  test("the logLevels bean applies coilix.logging and changes levels at runtime") {
    CoilixTestSetup
      .forPackages("dev.coilix.logging.none")
      .withProperties("coilix.logging.levels.com.example.noisy" -> "off")
      .withIORuntime(munitIORuntime)
      .useIO {
        val levels = bean[LogLevels]
        val shop   = LoggerFactory.getLogger("com.example.shop.Orders")
        for {
          silenced <- levels.levelOf("com.example.noisy.Chatter")
          _        <- levels.set("com.example.shop", LogLevel.Debug)
          _        <- IO(shop.debug("visible"))
          during   <- levels.levelOf("com.example.shop.Orders")
          _        <- levels.reset("com.example.shop")
          _        <- IO(shop.debug("hidden"))
          _        <- levels.setRoot(LogLevel.Warn)
          root     <- levels.root
          named    <- levels.levels
        } yield {
          assertEquals((silenced, during, root), (LogLevel.Off, LogLevel.Debug, LogLevel.Warn))
          assertEquals(named, Map("com.example.noisy" -> LogLevel.Off))
          assertEquals(recording.messages.filter(_.contains("com.example.shop")), Vector("DEBUG com.example.shop.Orders visible"))
        }
      }
  }
}
