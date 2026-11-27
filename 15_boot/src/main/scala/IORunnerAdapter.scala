package dev.coilix.boot

import cats.effect.IO
import dev.coilix.context.ApplicationContext

/**
 * Runs the [[dev.coilix.boot.IOApplicationRunner]] beans among the [[dev.coilix.boot.ApplicationRunner]] beans. The boot module
 * registers it as the bean `ioRunnerAdapter`.
 */
case class IORunnerAdapter() extends RunnerAdapter {

  override def runners(context: ApplicationContext): List[AnyRef] =
    context.getBeansOfType[IOApplicationRunner]

  override def run(runner: AnyRef, arguments: ApplicationArguments): IO[Unit] =
    runner match {
      case effectful: IOApplicationRunner => effectful.run(arguments)
      case _ => IO.unit
    }
}
