package dev.coilix.boot

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.Resource
import cats.effect.unsafe.IORuntime
import cats.syntax.all.*
import dev.coilix.annotation.Slf4s
import dev.coilix.boot.StartupEvents
import dev.coilix.concurrency.CatsEffectSettings
import dev.coilix.concurrency.FatalErrorHandler
import dev.coilix.concurrency.FatalErrors
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
   * The context of application `T`, built with `runtime` as its `ioRuntime` bean, without running the runners. While the
   * resource is in use, the [[dev.coilix.concurrency.FatalErrorHandler]] of the context is the default uncaught exception
   * handler of the process, so a fatal error in any thread ends the application.
   *
   * @tparam T
   *   the class annotated with `@CoilixApplication`
   */
  def application[T: ClassTag](args: List[String], runtime: IORuntime): Resource[IO, StartedApplication] =
    Resource
      .make(IO.blocking(built[T](args, runtime)))(application => IO.blocking(application.context.close()))
      .flatTap(application => fatalErrorsOf(application))

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

  private def fatalErrorsOf(application: StartedApplication): Resource[IO, Unit] =
    application.context.getBeanOption[FatalErrorHandler].fold(Resource.unit[IO])(handler =>
      Resource.make(IO(installed(handler)))(previous => IO(restored(previous))).void
    )

  private def installed(handler: FatalErrorHandler): Option[Thread.UncaughtExceptionHandler] = {
    val previous = Option(Thread.getDefaultUncaughtExceptionHandler)
    Thread.setDefaultUncaughtExceptionHandler(FatalErrors.uncaughtExceptionHandler(handler))
    previous
  }

  /** Restores the previous default handler; `null`, the value of the JDK for none, when there was none. */
  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  private def restored(previous: Option[Thread.UncaughtExceptionHandler]): Unit =
    Thread.setDefaultUncaughtExceptionHandler(previous.orNull)

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
