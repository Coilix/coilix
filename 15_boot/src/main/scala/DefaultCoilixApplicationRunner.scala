package dev.coilix.boot

import dev.coilix.common.ClassTags.runtimeTypeOf
import dev.coilix.config.ComponentProvider
import dev.coilix.config.Environment
import dev.coilix.config.EnvironmentLoader
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.context.RegisteredBean
import scala.concurrent.duration.DurationLong
import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag

/**
 * Runs the startup sequence: parses the arguments, loads the configuration with command-line options as overrides, logs the
 * start, builds the context, registers the shutdown hook, logs the startup time and calls the runners.
 *
 * The arguments and the configuration are registered as beans named `applicationArguments` and `environment`. This is the
 * implementation returned by [[dev.coilix.boot.CoilixApplicationRunner.default]].
 *
 * @param parser
 *   splits the command-line arguments
 * @param environmentLoader
 *   loads the configuration
 * @param assemble
 *   creates the other startup components from the loaded configuration, so they can be replaced there
 */
final class DefaultCoilixApplicationRunner(
  parser:            ArgumentsParser,
  environmentLoader: EnvironmentLoader,
  assemble: ComponentProvider => StartupComponents
) extends CoilixApplicationRunner {

  private val argumentsBeanName   = "applicationArguments"
  private val environmentBeanName = "environment"

  override def run[T: ClassTag](args: List[String]): ConfigurableApplicationContext = {
    val startedAt   = System.nanoTime()
    val arguments   = parser.parse(args)
    val environment = environmentLoader.load(commandLineProperties(arguments))
    val startup     = assemble(ComponentProvider.from(environment))
    startup.startupLogger.starting(applicationName[T])
    val context     = startup.bootstrap.start(startup.resolver.resolve[T], Nil, singletons(arguments, environment))
    val _           = startup.shutdownHooks.register(context)
    startup.startupLogger.started(applicationName[T], elapsedSince(startedAt))
    startup.invoker.invoke(context, arguments)
    context
  }

  private def commandLineProperties(arguments: ApplicationArguments): Map[String, String] =
    arguments.options.collect { case (name, value :: _) => name -> value }

  private def singletons(arguments: ApplicationArguments, environment: Environment): List[RegisteredBean] =
    List(
      RegisteredBean(argumentsBeanName, classOf[ApplicationArguments], isPrimary = false, arguments),
      RegisteredBean(environmentBeanName, environment.getClass, isPrimary        = false, environment)
    )

  private def applicationName[T: ClassTag]: String =
    runtimeTypeOf[T].getSimpleName.stripSuffix("$")

  private def elapsedSince(startedAt: Long): FiniteDuration = (System.nanoTime() - startedAt).nanos
}
