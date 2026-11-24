package dev.coilix.boot

import dev.coilix.annotation.Order
import dev.coilix.annotation.Slf4s
import dev.coilix.context.ApplicationContext

/**
 * Calls the [[dev.coilix.boot.ApplicationRunner]] beans and the runners of every [[dev.coilix.boot.RunnerAdapter]] bean in one
 * list sorted by the `@Order` value of their classes, one after another. Runners without `@Order` run last, and runners with the
 * same value keep their creation order.
 *
 * This is the default [[dev.coilix.boot.RunnerInvoker]].
 */
@Slf4s
final class OrderedRunnerInvoker extends RunnerInvoker {

  override def invoke(context: ApplicationContext, arguments: ApplicationArguments): Unit =
    ordered(context, runners(context)).foreach((runner, run) => invokeRunner(runner, run, arguments))

  private def runners(context: ApplicationContext): List[(AnyRef, ApplicationArguments => Unit)] =
    context.getBeansOfType[ApplicationRunner].map(runner => runner -> runner.run) ++
      context.getBeansOfType[RunnerAdapter].flatMap(adapted(context))

  private def adapted(context: ApplicationContext)(adapter: RunnerAdapter): List[(AnyRef, ApplicationArguments => Unit)] =
    adapter.runners(context).map(runner => runner -> (arguments => adapter.run(runner, arguments)))

  private def ordered(
    context: ApplicationContext,
    runners: List[(AnyRef, ApplicationArguments => Unit)]
  ): List[(AnyRef, ApplicationArguments => Unit)] = {
    val created = context.createdBeans.map(_.instance)
    runners.sortBy((runner, _) => (orderOf(runner), created.indexWhere(_ eq runner)))
  }

  private def orderOf(runner: AnyRef): Int =
    Option(runner.getClass.getAnnotation(classOf[Order])).map(_.value).getOrElse(Order.LOWEST_PRECEDENCE)

  private def invokeRunner(runner: AnyRef, run: ApplicationArguments => Unit, arguments: ApplicationArguments): Unit = {
    log.debug(s"Running ${runner.getClass.getSimpleName}")
    run(arguments)
  }
}
