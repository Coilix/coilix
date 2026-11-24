package dev.coilix.boot

import dev.coilix.annotation.Slf4s
import dev.coilix.context.ApplicationContext

/**
 * Calls the [[dev.coilix.boot.ApplicationRunner]] beans and the runners of every [[dev.coilix.boot.RunnerAdapter]] bean one after
 * another, in the order of [[dev.coilix.boot.OrderedRunner.all]]: by the `@Order` value of their classes, runners without it last
 * and runners with the same value in creation order.
 *
 * This is the default [[dev.coilix.boot.RunnerInvoker]].
 */
@Slf4s
final class OrderedRunnerInvoker extends RunnerInvoker {

  override def invoke(context: ApplicationContext, arguments: ApplicationArguments): Unit =
    OrderedRunner.all(context).foreach(entry => invokeRunner(entry, arguments))

  private def invokeRunner(entry: OrderedRunner, arguments: ApplicationArguments): Unit = {
    log.debug(s"Running ${entry.runner.getClass.getSimpleName}")
    entry.run(arguments)
  }
}
