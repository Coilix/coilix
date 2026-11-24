package dev.coilix.catseffect

import cats.effect.unsafe.IORuntime
import dev.coilix.boot.ApplicationArguments
import dev.coilix.boot.RunnerAdapter
import dev.coilix.context.ApplicationContext

/**
 * Runs the [[dev.coilix.catseffect.IOApplicationRunner]] beans among the [[dev.coilix.boot.ApplicationRunner]] beans, waiting
 * for each effect on `runtime`. The cats-effect module registers it as the bean `ioRunnerAdapter`.
 *
 * @param runtime
 *   runs the effects of the runners
 */
final class IORunnerAdapter(runtime: IORuntime) extends RunnerAdapter {

  override def runners(context: ApplicationContext): List[AnyRef] =
    context.getBeansOfType[IOApplicationRunner]

  override def run(runner: AnyRef, arguments: ApplicationArguments): Unit =
    runner match {
      case effectful: IOApplicationRunner => effectful.run(arguments).unsafeRunSync()(using runtime)
      case _ => ()
    }
}
