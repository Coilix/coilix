package dev.coilix.boot

import dev.coilix.annotation.Order
import dev.coilix.annotation.Slf4s
import dev.coilix.context.ApplicationContext

/**
 * Calls the [[dev.coilix.boot.ApplicationRunner]] beans sorted by their `@Order` value. Runners without `@Order` run last, and
 * runners
 * with the same value keep their creation order.
 *
 * This is the default [[dev.coilix.boot.RunnerInvoker]].
 */
@Slf4s
final class OrderedRunnerInvoker extends RunnerInvoker {

  override def invoke(context: ApplicationContext, arguments: ApplicationArguments): Unit =
    ordered(context.getBeansOfType[ApplicationRunner]).foreach(runner => invokeRunner(runner, arguments))

  private def ordered(runners: List[ApplicationRunner]): List[ApplicationRunner] =
    runners.sortBy(orderOf)

  private def orderOf(runner: ApplicationRunner): Int =
    Option(runner.getClass.getAnnotation(classOf[Order])).map(_.value).getOrElse(Order.LOWEST_PRECEDENCE)

  private def invokeRunner(runner: ApplicationRunner, arguments: ApplicationArguments): Unit = {
    log.debug(s"Running ${runner.getClass.getSimpleName}")
    runner.run(arguments)
  }
}
