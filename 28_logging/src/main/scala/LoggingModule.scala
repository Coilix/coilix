package dev.coilix.logging

import dev.coilix.context.CoilixModule

/**
 * Adds the `loggingSettings` and `logLevels` beans. The module is also the SLF4J provider of the application, so the
 * application, and every library that logs through SLF4J, `java.util.logging`, Log4j 2 or Commons Logging, logs in its format
 * with the levels of `coilix.logging`. It can be disabled with `coilix.modules.logging.enabled = false`, which keeps the
 * provider with the levels read at startup.
 */
final class LoggingModule extends CoilixModule {

  override def name: String = "logging"

  override def components: List[Class[?]] = List(classOf[LoggingConfiguration])
}
