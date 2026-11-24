package dev.coilix.catseffect

import cats.effect.IO
import dev.coilix.boot.ApplicationArguments

/**
 * A bean that runs an `IO` once the application context is ready.
 *
 * Runners of this kind run in one list with the [[dev.coilix.boot.ApplicationRunner]] beans, in the order set by `@Order`
 * (runners without it run last), each after the previous one finishes. The effect runs on the `ioRuntime` bean. A failure of the
 * effect stops the startup, as an exception thrown by an [[dev.coilix.boot.ApplicationRunner]] does.
 *
 * ```scala
 * @Component
 * @Order(2)
 * final class CacheWarmup(catalog: Catalog) extends IOApplicationRunner {
 *   override def run(arguments: ApplicationArguments): IO[Unit] =
 *     catalog.categories.flatMap(_.parTraverse_(catalog.load))
 * }
 * ```
 */
trait IOApplicationRunner {

  /** Describes the startup logic with the command-line arguments of the application. */
  def run(arguments: ApplicationArguments): IO[Unit]
}
