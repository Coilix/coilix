package dev.coilix.logging

import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration
import dev.coilix.annotation.Slf4s
import dev.coilix.config.Environment

/**
 * The beans of the `logging` module: `loggingSettings` and `logLevels`. Creating `logLevels` applies the settings of the
 * context, with command-line options, to every logger. When another SLF4J provider is on the classpath and SLF4J chose it, the
 * settings and runtime changes reach only the loggers of this module, and a warning says so.
 */
@Slf4s
@Configuration
final class LoggingConfiguration {

  /** The settings under `coilix.logging`, or the defaults in a context without an environment. */
  @Bean
  def loggingSettings(environment: Option[Environment]): LoggingSettings =
    environment.fold(LoggingSettings())(LoggingSettings.from)

  /** Reads and changes the levels of loggers at runtime, after applying `settings`. */
  @Bean
  def logLevels(settings: LoggingSettings): FactoryLogLevels = {
    if (!CoilixLogging.isActive) {
      log.warn("coilix-logging is on the classpath, but SLF4J uses another provider, so coilix.logging does not apply")
    }
    CoilixLogging.configure(settings)
    FactoryLogLevels(CoilixLogging.factory, JulBridge.follow)
  }
}
