package io.github.coilix.boot

import io.github.coilix.annotation.Order
import io.github.coilix.annotation.Slf4s
import io.github.coilix.context.ApplicationContext

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
