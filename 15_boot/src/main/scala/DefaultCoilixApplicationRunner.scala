package dev.coilix.boot

import dev.coilix.annotation.Slf4s
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
 * start, builds the context, registers the shutdown hook, logs the startup time and calls the runners between the
 * [[dev.coilix.events.ContextReady]] and [[dev.coilix.events.ApplicationStarted]] events. When the runners or the listeners of
 * these events fail, [[dev.coilix.events.ApplicationFailed]] is published and the context is closed before the exception is
 * rethrown. Options of the JVM that make the application fragile when memory runs out, described in
 * [[dev.coilix.boot.JvmMemoryOptions]], are logged at `WARN` level at the start.
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
@Slf4s
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
    JvmMemoryOptions.current.warnings.foreach(warning => log.warn(warning))
    val context     = startup.bootstrap.start(startup.resolver.resolve[T], Nil, singletons(arguments, environment))
    val _           = startup.shutdownHooks.register(context)
    startup.startupLogger.started(applicationName[T], elapsedSince(startedAt))
    StartupEvents.around(context)(startup.invoker.invoke(context, arguments))
  }

  private def commandLineProperties(arguments: ApplicationArguments): Map[String, String] =
    arguments.options.collect { case (name, value :: _) => name -> value }

  private def singletons(arguments: ApplicationArguments, environment: Environment): List[RegisteredBean] =
    List(
      RegisteredBean.of[ApplicationArguments](argumentsBeanName, arguments),
      RegisteredBean.ofClass(environmentBeanName, environment.getClass, environment)
    )

  private def applicationName[T: ClassTag]: String =
    runtimeTypeOf[T].getSimpleName.stripSuffix("$")

  private def elapsedSince(startedAt: Long): FiniteDuration = (System.nanoTime() - startedAt).nanos
}
