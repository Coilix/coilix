package dev.coilix.testkit

import dev.coilix.boot.ApplicationArguments
import dev.coilix.boot.ArgumentsParser
import dev.coilix.boot.BeanReplacements
import dev.coilix.boot.StartupComponents
import dev.coilix.config.ComponentProvider
import dev.coilix.config.Environment
import dev.coilix.config.EnvironmentLoader
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.context.RegisteredBean
import scala.util.Failure
import scala.util.Success
import scala.util.Try

/**
 * Builds a test context the way `Coilix.run` builds an application, without the banner, the startup log and the shutdown hook.
 *
 * The configuration is loaded with the setup properties and profiles over the command-line options, so they win over every
 * other source. The arguments and the configuration are registered as `applicationArguments` and `environment`, and the beans of
 * the setup are registered under their names. When the runners fail, the context is closed before the error is rethrown. This is
 * the implementation returned by [[dev.coilix.testkit.TestContextFactory.default]].
 *
 * @param parser
 *   splits the command-line arguments
 * @param environmentLoader
 *   loads the configuration
 * @param assemble
 *   creates the startup components from the loaded configuration
 */
final class DefaultTestContextFactory(
  parser:            ArgumentsParser,
  environmentLoader: EnvironmentLoader,
  assemble: ComponentProvider => StartupComponents
) extends TestContextFactory {

  override def create(setup: CoilixTestSetup): ConfigurableApplicationContext = {
    val arguments   = parser.parse(setup.arguments)
    val environment = environmentLoader.load(commandLineProperties(arguments) ++ setup.configurationOverrides)
    val startup     = assemble(ComponentProvider.from(environment))
    val context     = startup.bootstrap.start(
      setup.packages(startup.resolver),
      Nil,
      singletons(arguments, environment) ++ setup.beans.map(registered),
      BeanReplacements(setup.beans.filter(_.replaces).map(_.beanType))
    )
    if (setup.runners) withRunners(startup, context, arguments) else context
  }

  private def withRunners(
    startup:   StartupComponents,
    context:   ConfigurableApplicationContext,
    arguments: ApplicationArguments
  ): ConfigurableApplicationContext =
    Try(startup.invoker.invoke(context, arguments)) match {
      case Success(_)     => context
      case Failure(error) => closedAfter(context, error)
    }

  private def closedAfter(context: ConfigurableApplicationContext, error: Throwable): Nothing = {
    context.close()
    throw error
  }

  private def commandLineProperties(arguments: ApplicationArguments): Map[String, String] =
    arguments.options.collect { case (name, value :: _) => name -> value }

  private def singletons(arguments: ApplicationArguments, environment: Environment): List[RegisteredBean] =
    List(
      RegisteredBean.of[ApplicationArguments]("applicationArguments", arguments),
      RegisteredBean.ofClass("environment", environment.getClass, environment)
    )

  private def registered(bean: TestBean): RegisteredBean =
    RegisteredBean(bean.beanName, bean.beanType, isPrimary = false, bean.instance)
}
