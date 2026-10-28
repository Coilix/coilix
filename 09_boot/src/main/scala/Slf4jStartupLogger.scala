package org.scaling.boot

import org.scaling.annotation.Slf4s
import scala.concurrent.duration.FiniteDuration

@Slf4s
final class Slf4jStartupLogger(banner: BannerLoader, formatter: StartupMessageFormatter) extends StartupLogger {

  override def starting(applicationName: String): Unit = {
    banner.load().foreach(text => log.info(s"\n$text"))
    log.info(formatter.starting(applicationName))
  }

  override def started(applicationName: String, elapsed: FiniteDuration): Unit =
    log.info(formatter.started(applicationName, elapsed))
}
