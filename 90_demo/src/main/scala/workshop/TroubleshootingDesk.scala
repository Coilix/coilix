package dev.coilix.demo
package workshop

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.logging.LogLevel
import dev.coilix.logging.LogLevels

/**
 * Turns on the detailed logs of the background processors while a mechanic investigates a stuck export, and turns them off
 * again, without restarting the workshop. The levels the workshop starts with are in `coilix.logging` of `application.yaml`.
 */
@Component
final class TroubleshootingDesk(levels: LogLevels) {

  private val processors = "dev.coilix.processing"

  /** Logs the processors at `DEBUG` and returns the level the processor manager now uses. */
  def investigateProcessors: IO[LogLevel] =
    levels.set(processors, LogLevel.Debug) *> levels.levelOf(s"$processors.DefaultProcessorManager")

  /** Returns the processors to the level of the configuration. */
  def closeInvestigation: IO[LogLevel] =
    levels.reset(processors) *> levels.levelOf(s"$processors.DefaultProcessorManager")
}
