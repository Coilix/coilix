package dev.coilix.demo
package workshop

import dev.coilix.annotation.Component
import dev.coilix.annotation.Slf4s
import dev.coilix.boot.ApplicationArguments
import dev.coilix.boot.ApplicationRunner
import dev.coilix.concurrency.TaskExecutor

/**
 * Simulates a diagnostic tester that runs out of memory, started with `--simulate-fatal-error`.
 *
 * The task throws an `OutOfMemoryError` on the [[dev.coilix.concurrency.TaskExecutor]]. Coilix treats it as fatal: the
 * `fatalErrorHandler` closes the context and the process ends with exit code 70, set by `coilix.fatal-errors.exit-code`.
 * Without the option the drill does nothing.
 */
@Component
@Slf4s
final class TesterCrashDrill(executor: TaskExecutor) extends ApplicationRunner {

  override def run(arguments: ApplicationArguments): Unit =
    if (arguments.containsOption("simulate-fatal-error")) crash()

  private def crash(): Unit = {
    log.warn("Simulating a tester that runs out of memory")
    executor.execute(throw OutOfMemoryError("Simulated: the diagnostic tester ran out of memory"))
  }
}
