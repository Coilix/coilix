package dev.coilix.boot

import cats.effect.IO
import cats.effect.Resource
import dev.coilix.annotation.Slf4s
import dev.coilix.common.ClassTags.runtimeTypeOf
import dev.coilix.config.ComponentProvider
import dev.coilix.config.Environment
import dev.coilix.config.EnvironmentLoader
import dev.coilix.context.RegisteredBean
import scala.reflect.ClassTag

/**
 * Builds the context of an application: parses the arguments, loads the configuration with command-line options as overrides,
 * logs the start, builds the context as a `Resource` and logs the startup time. Parsing, loading and scanning run in
 * `IO.blocking`. Options of the JVM that make the application fragile when
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

  override def start[T: ClassTag](args: List[String], singletons: List[RegisteredBean]): Resource[IO, StartedApplication] =
    for {
      startedAt   <- Resource.eval(IO.monotonic)
      arguments   <- Resource.eval(IO(parser.parse(args)))
      environment <- Resource.eval(IO.blocking(environmentLoader.load(commandLineProperties(arguments))))
      startup     <- Resource.eval(IO.blocking(starting[T](environment)))
      context     <- startup.bootstrap.start(startup.resolver.resolve[T], Nil, startupBeans(arguments, environment) ++ singletons)
      elapsed     <- Resource.eval(IO.monotonic.map(_ - startedAt))
      _           <- Resource.eval(IO(startup.startupLogger.started(applicationName[T], elapsed)))
    } yield StartedApplication(context, arguments)

  private def starting[T: ClassTag](environment: Environment): StartupComponents = {
    val startup = assemble(ComponentProvider.from(environment))
    startup.startupLogger.starting(applicationName[T])
    JvmMemoryOptions.current.warnings.foreach(warning => log.warn(warning))
    startup
  }

  private def commandLineProperties(arguments: ApplicationArguments): Map[String, String] =
    arguments.options.collect { case (name, value :: _) => name -> value }

  private def startupBeans(arguments: ApplicationArguments, environment: Environment): List[RegisteredBean] =
    List(
      RegisteredBean.of[ApplicationArguments](argumentsBeanName, arguments),
      RegisteredBean.ofClass(environmentBeanName, environment.getClass, environment)
    )

  private def applicationName[T: ClassTag]: String =
    runtimeTypeOf[T].getSimpleName.stripSuffix("$")
}
