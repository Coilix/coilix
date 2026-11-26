package dev.coilix.boot

import cats.effect.IO
import cats.syntax.all.*
import dev.coilix.annotation.Slf4s
import dev.coilix.context.ApplicationContext

/**
 * Runs the [[dev.coilix.boot.ApplicationRunner]] beans and the runners of every [[dev.coilix.boot.RunnerAdapter]] bean one after
 * another, in the order of [[dev.coilix.boot.OrderedRunner.all]]: by the `@Order` value of their classes, runners without it last
 * and runners with the same value in creation order. A failing runner stops the runners after it.
 *
 * This is the default [[dev.coilix.boot.RunnerInvoker]].
 */
@Slf4s
final class OrderedRunnerInvoker extends RunnerInvoker {

  override def invoke(context: ApplicationContext, arguments: ApplicationArguments): IO[Unit] =
    IO(OrderedRunner.all(context)).flatMap(_.traverse_(entry => invokeRunner(entry, arguments)))

  private def invokeRunner(entry: OrderedRunner, arguments: ApplicationArguments): IO[Unit] =
    logF.debug(s"Running ${entry.runner.getClass.getSimpleName}") *> entry.run(arguments)
}
