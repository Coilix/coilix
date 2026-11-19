package dev.coilix.boot

import dev.coilix.annotation.Slf4s
import scala.concurrent.duration.FiniteDuration

/**
 * Logs the banner and the startup messages through SLF4J at `INFO` level.
 *
 * This is the default [[dev.coilix.boot.StartupLogger]].
 *
 * @param banner
 *   loads the banner logged before the first message
 * @param formatter
 *   formats the messages
 */
@Slf4s
final class Slf4jStartupLogger(banner: BannerLoader, formatter: StartupMessageFormatter) extends StartupLogger {

  override def starting(applicationName: String): Unit = {
    banner.load().foreach(text => log.info(s"\n$text"))
    log.info(formatter.starting(applicationName))
  }

  override def started(applicationName: String, elapsed: FiniteDuration): Unit =
    log.info(formatter.started(applicationName, elapsed))
}
