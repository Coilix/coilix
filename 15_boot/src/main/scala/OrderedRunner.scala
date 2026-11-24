package dev.coilix.boot

import dev.coilix.annotation.Order
import dev.coilix.context.ApplicationContext

/**
 * One runner of the application: an [[dev.coilix.boot.ApplicationRunner]] bean or a runner of a
 * [[dev.coilix.boot.RunnerAdapter]], with the way to run it.
 *
 * @param runner
 *   the runner bean, whose class carries its `@Order`
 * @param run
 *   runs the runner and returns when it finishes
 */
final case class OrderedRunner(runner: AnyRef, run: ApplicationArguments => Unit)

/** The runners of a context in the order in which they run. */
object OrderedRunner {

  /**
   * The [[dev.coilix.boot.ApplicationRunner]] beans of `context` and the runners of its [[dev.coilix.boot.RunnerAdapter]] beans,
   * sorted by the `@Order` value of their classes. Runners without `@Order` come last, and runners with the same value keep their
   * creation order.
   */
  def all(context: ApplicationContext): List[OrderedRunner] = {
    val runners = plain(context) ++ context.getBeansOfType[RunnerAdapter].flatMap(adapted(context))
    val created = context.createdBeans.map(_.instance)
    runners.sortBy(entry => (orderOf(entry.runner), created.indexWhere(_ eq entry.runner)))
  }

  private def plain(context: ApplicationContext): List[OrderedRunner] =
    context.getBeansOfType[ApplicationRunner].map(runner => OrderedRunner(runner, runner.run))

  private def adapted(context: ApplicationContext)(adapter: RunnerAdapter): List[OrderedRunner] =
    adapter.runners(context).map(runner => OrderedRunner(runner, arguments => adapter.run(runner, arguments)))

  private def orderOf(runner: AnyRef): Int =
    Option(runner.getClass.getAnnotation(classOf[Order])).map(_.value).getOrElse(Order.LOWEST_PRECEDENCE)
}
