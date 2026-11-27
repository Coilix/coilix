package dev.coilix.testkit

import cats.effect.IO
import cats.effect.Resource
import dev.coilix.boot.ApplicationArguments
import dev.coilix.boot.ArgumentsParser
import dev.coilix.boot.BeanReplacements
import dev.coilix.boot.StartupComponents
import dev.coilix.boot.StartupError
import dev.coilix.boot.StartupEvents
import dev.coilix.concurrency.FatalErrorSettings
import dev.coilix.config.ComponentProvider
import dev.coilix.config.Environment
import dev.coilix.config.EnvironmentLoader
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.context.RegisteredBean

/**
 * Builds a test context the way a `CoilixIOApp` builds an application, without the banner and the startup log.
 *
 * The configuration is loaded with the setup properties and profiles over the command-line options, so they win over every
 * other source. Fatal errors are only logged (`coilix.fatal-errors.action = log`), so a test does not end the JVM, unless the
 * setup sets the action. The arguments and the configuration are registered as `applicationArguments` and `environment`, and the beans of
 * the setup are registered under their names, next to an [[dev.coilix.testkit.EventRecorder]] named `eventRecorder`. The context
 * publishes [[dev.coilix.events.ContextReady]] and [[dev.coilix.events.ApplicationStarted]] like an application, with the runners
 * between them when the setup asks for them. When the runners or the listeners of these events fail, the context is closed
 * before the resource fails. A test is the top layer of its context, so a [[dev.coilix.boot.StartupError]] or a
 * [[dev.coilix.config.ConfigError]] fails the resource with its exception. This is the implementation returned by
 * [[dev.coilix.testkit.TestContextFactory.default]].
 *
 * @param parser
 *   splits the command-line arguments
 * @param environmentLoader
 *   loads the configuration
 * @param assemble
 *   creates the startup components from the loaded configuration
 */
case class DefaultTestContextFactory(
  parser:            ArgumentsParser,
  environmentLoader: EnvironmentLoader,
  assemble: ComponentProvider => StartupComponents
) extends TestContextFactory {

  override def resource(setup: CoilixTestSetup): Resource[IO, ConfigurableApplicationContext] =
    for {
      arguments   <- Resource.eval(IO(parser.parse(setup.arguments)))
      environment <- Resource.eval(loaded(setup, arguments))
      startup     <- Resource.eval(IO(assemble(ComponentProvider.from(environment))))
      packages    <- Resource.eval(IO(setup.packages(startup.resolver)))
      context     <- StartupError.raised(
                       startup.bootstrap.start(
                         packages,
                         Nil,
                         singletons(arguments, environment) ++ setup.beans.map(registered),
                         BeanReplacements(setup.beans.filter(_.replaces).map(_.beanType))
                       )
                     )
      _           <- Resource.eval(context.start *> StartupEvents.around(context)(runners(setup, startup, context, arguments)))
    } yield context

  private def loaded(setup: CoilixTestSetup, arguments: ApplicationArguments): IO[Environment] =
    IO.blocking(environmentLoader.load(properties(setup, arguments))).flatMap(loaded => IO.fromEither(loaded.left.map(_.exception)))

  private def properties(setup: CoilixTestSetup, arguments: ApplicationArguments): Map[String, String] =
    safeDefaults ++ commandLineProperties(arguments) ++ setup.configurationOverrides

  private def runners(
    setup:     CoilixTestSetup,
    startup:   StartupComponents,
    context:   ConfigurableApplicationContext,
    arguments: ApplicationArguments
  ): IO[Unit] =
    if (setup.runners) startup.invoker.invoke(context, arguments) else IO.unit

  private val safeDefaults: Map[String, String] = Map(FatalErrorSettings.path + ".action" -> "log")

  private def commandLineProperties(arguments: ApplicationArguments): Map[String, String] =
    arguments.options.collect { case (name, value :: _) => name -> value }

  private def singletons(arguments: ApplicationArguments, environment: Environment): List[RegisteredBean] =
    List(
      RegisteredBean.of[ApplicationArguments]("applicationArguments", arguments),
      RegisteredBean.ofClass("environment", environment.getClass, environment),
      RegisteredBean.of[EventRecorder]("eventRecorder", EventRecorder())
    )

  private def registered(bean: TestBean): RegisteredBean =
    RegisteredBean(bean.beanName, bean.beanType, isPrimary = false, bean.instance)
}
