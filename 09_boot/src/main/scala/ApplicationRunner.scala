package dev.coilix.boot

/**
 * A bean that runs once the application context is ready.
 *
 * Every bean that implements this trait is called by `Coilix.run` after all beans are created, in the order set by `@Order`
 * (runners without it run last). An exception thrown by a runner stops the startup and is rethrown from `Coilix.run`.
 *
 * ```scala
 * @Component
 * @Order(1)
 * final class MigrationRunner(migrations: Migrations) extends ApplicationRunner {
 *   override def run(arguments: ApplicationArguments): Unit =
 *     if (!arguments.containsOption("skip-migrations")) migrations.apply()
 * }
 * ```
 */
trait ApplicationRunner {

  /** Runs the startup logic with the command-line arguments of the application. */
  def run(arguments: ApplicationArguments): Unit
}
