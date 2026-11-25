package dev.coilix.boot

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.Resource
import cats.effect.unsafe.IORuntime
import cats.syntax.foldable.*
import dev.coilix.annotation.Slf4s
import dev.coilix.boot.StartupEvents
import dev.coilix.concurrency.CatsEffectSettings
import dev.coilix.context.RegisteredBean
import scala.reflect.ClassTag

/**
 * Starts an application in `IO`, as [[dev.coilix.boot.CoilixIOApp]] does.
 *
 * The context is a `Resource`: it is built with the given runtime registered as the `ioRuntime` bean, so the beans of the
 * concurrency module do not create a runtime of their own, and it closes when the resource is released, also when the fiber is
 * cancelled. The runners run between [[dev.coilix.events.ContextReady]] and [[dev.coilix.events.ApplicationStarted]] in the order
 * of [[dev.coilix.boot.OrderedRunner.all]]: an [[dev.coilix.boot.IOApplicationRunner]] as its `IO`, any other runner in
 * `IO.blocking`. A failing runner is published as [[dev.coilix.events.ApplicationFailed]] and fails the start after the context
 * closes.
 */
@Slf4s
object CoilixIOStartup {

  private val runtimeBeanName = "ioRuntime"

  /**
   * The context of application `T`, built with `runtime` as its `ioRuntime` bean, without running the runners.
   *
   * @tparam T
   *   the class annotated with `@CoilixApplication`
   */
  def application[T: ClassTag](args: List[String], runtime: IORuntime): Resource[IO, StartedApplication] =
    Resource.make(IO.blocking(built[T](args, runtime)))(application => IO.blocking(application.context.close()))

  /**
   * Starts application `T`, runs its runners and then, unless `coilix.cats-effect.exit-after-runners` is `true`, runs until it
   * is cancelled. The context closes before the returned `IO` completes.
   *
   * @tparam T
   *   the class annotated with `@CoilixApplication`
   * @return
   *   [[cats.effect.ExitCode.Success]] after the context closes; a failure of the start or of a runner fails the `IO`
   */
  def run[T: ClassTag](args: List[String], runtime: IORuntime): IO[ExitCode] =
    application[T](args, runtime).use(application => runners(application) *> awaited(application)).as(ExitCode.Success)

  /**
   * Runs the runners of `application` between the startup events, as [[run]] does.
   *
   * @return
   *   an `IO` that fails with the failure of a runner or of a listener of the startup events
   */
  def runners(application: StartedApplication): IO[Unit] = {
    val context = application.context
    (IO.blocking(StartupEvents.ready(context)) *>
      OrderedRunner.all(context).traverse_(runner => runOne(application, runner)) *>
      IO.blocking(StartupEvents.started(context)))
      .onError(failure => IO.blocking(StartupEvents.failure(context, failure)))
  }

  private def built[T: ClassTag](args: List[String], runtime: IORuntime): StartedApplication =
    CoilixApplicationRunner.default.start[T](args, List(RegisteredBean.of[IORuntime](runtimeBeanName, runtime)))

  private def runOne(application: StartedApplication, entry: OrderedRunner): IO[Unit] =
    IO(log.debug(s"Running ${entry.runner.getClass.getSimpleName}")) *>
      (entry.runner match {
        case runner: IOApplicationRunner => runner.run(application.arguments)
        case _ => IO.blocking(entry.run(application.arguments))
      })

  private def awaited(application: StartedApplication): IO[Unit] =
    if (settingsOf(application).exitAfterRunners) IO.unit else IO.never

  private def settingsOf(application: StartedApplication): CatsEffectSettings =
    application.context.getBeanOption[CatsEffectSettings].getOrElse(CatsEffectSettings())
}
