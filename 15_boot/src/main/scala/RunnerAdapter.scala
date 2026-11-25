package dev.coilix.boot

import dev.coilix.context.ApplicationContext

/**
 * Runs beans of another kind of runner among the [[dev.coilix.boot.ApplicationRunner]] beans, for a module whose runners have a
 * different signature, such as [[dev.coilix.boot.IOApplicationRunner]].
 *
 * Every bean of this type is used by the [[dev.coilix.boot.OrderedRunnerInvoker]], which puts the runners it gives into one list
 * with the [[dev.coilix.boot.ApplicationRunner]] beans, ordered by the `@Order` of their classes.
 *
 * ```scala
 * @Component
 * final class ScriptRunnerAdapter extends RunnerAdapter {
 *   override def runners(context: ApplicationContext): List[AnyRef] = context.getBeansOfType[Script]
 *
 *   override def run(runner: AnyRef, arguments: ApplicationArguments): Unit =
 *     runner match {
 *       case script: Script => script.execute(arguments.nonOptionArgs)
 *       case _              => ()
 *     }
 * }
 * ```
 */
trait RunnerAdapter {

  /** The runners of this kind in `context`, in creation order. */
  def runners(context: ApplicationContext): List[AnyRef]

  /** Runs `runner`, one of [[runners]], and returns when it finishes. */
  def run(runner: AnyRef, arguments: ApplicationArguments): Unit
}
