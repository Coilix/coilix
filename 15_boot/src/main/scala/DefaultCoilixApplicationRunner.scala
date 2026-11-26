package dev.coilix.boot

import dev.coilix.annotation.Slf4s
import dev.coilix.common.ClassTags.runtimeTypeOf
import dev.coilix.config.ComponentProvider
import dev.coilix.config.Environment
import dev.coilix.config.EnvironmentLoader
import dev.coilix.context.RegisteredBean
import scala.concurrent.duration.DurationLong
import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag

/**
 * Builds the context of an application: parses the arguments, loads the configuration with command-line options as overrides,
 * logs the start, builds the context and logs the startup time. Options of the JVM that make the application fragile when
 * memory runs out, described in [[dev.coilix.boot.JvmMemoryOptions]], are logged at `WARN` level at the start. The runners and
 * their events are left to the caller, such as [[dev.coilix.boot.CoilixIOStartup]].
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

  override def start[T: ClassTag](args: List[String], singletons: List[RegisteredBean]): StartedApplication =
    started[T](args, singletons)

  private def started[T: ClassTag](args: List[String], extra: List[RegisteredBean]): StartedApplication = {
    val startedAt   = System.nanoTime()
    val arguments   = parser.parse(args)
    val environment = environmentLoader.load(commandLineProperties(arguments))
    val startup     = assemble(ComponentProvider.from(environment))
    startup.startupLogger.starting(applicationName[T])
    JvmMemoryOptions.current.warnings.foreach(warning => log.warn(warning))
    val context     = startup.bootstrap.start(startup.resolver.resolve[T], Nil, singletons(arguments, environment) ++ extra)
    startup.startupLogger.started(applicationName[T], elapsedSince(startedAt))
    StartedApplication(context, arguments)
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
