package dev.coilix.boot

import scala.concurrent.duration.FiniteDuration
import scala.math.BigDecimal.RoundingMode

final class DefaultStartupMessageFormatter extends StartupMessageFormatter {

  override def starting(applicationName: String): String =
    s"Launching $applicationName"

  override def started(applicationName: String, elapsed: FiniteDuration): String =
    s"$applicationName is up after ${seconds(elapsed)} s"

  private def seconds(elapsed: FiniteDuration): String = (BigDecimal(elapsed.toMillis) / 1000).setScale(
    3,
    RoundingMode.HALF_UP
  ).toString
}
