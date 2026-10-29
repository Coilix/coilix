package io.github.coilix.boot

import io.github.coilix.common.ClassTags.runtimeTypeOf
import io.github.coilix.config.ComponentProvider
import io.github.coilix.config.Environment
import io.github.coilix.config.EnvironmentLoader
import io.github.coilix.context.ConfigurableApplicationContext
import io.github.coilix.context.RegisteredBean
import scala.concurrent.duration.DurationLong
import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag

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
