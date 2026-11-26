package dev.coilix.boot
package adapted

import cats.effect.IO
import dev.coilix.boot.ApplicationArguments
import dev.coilix.boot.RunnerAdapter
import dev.coilix.context.ApplicationContext

final class ScriptRunnerAdapter extends RunnerAdapter {

  override def runners(context: ApplicationContext): List[AnyRef] = context.getBeansOfType[Script]

  override def run(runner: AnyRef, arguments: ApplicationArguments): IO[Unit] =
    runner match {
      case script: Script => IO.blocking(script.execute(arguments.nonOptionArgs))
      case _ => IO.unit
    }
}
