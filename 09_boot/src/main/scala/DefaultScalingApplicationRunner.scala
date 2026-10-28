package org.scaling.boot

import org.scaling.common.ClassTags.runtimeTypeOf
import org.scaling.config.ComponentProvider
import org.scaling.config.Environment
import org.scaling.config.EnvironmentLoader
import org.scaling.context.ConfigurableApplicationContext
import org.scaling.context.RegisteredBean
import scala.concurrent.duration.DurationLong
import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag

final class DefaultScalingApplicationRunner(
  parser:            ArgumentsParser,
  environmentLoader: EnvironmentLoader,
  assemble: ComponentProvider => StartupComponents
) extends ScalingApplicationRunner {

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
