package dev.coilix.boot

import cats.data.EitherT
import cats.effect.IO
import cats.effect.Resource
import cats.syntax.all.*
import dev.coilix.annotation.Slf4s
import dev.coilix.common.ClassTags.runtimeTypeOf
import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigurationException
import dev.coilix.config.Environment
import dev.coilix.config.EnvironmentLoader
import dev.coilix.context.RegisteredBean
import dev.coilix.context.ResourceT
import scala.reflect.ClassTag

/**
 * Builds the context of an application: parses the arguments, loads the configuration with command-line options as overrides,
 * logs the start, builds the context as a `Resource` and logs the startup time. Parsing, loading and scanning run in
 * `IO.blocking`. A [[dev.coilix.config.ConfigurationException]] thrown while the configuration is loaded becomes a
 * [[dev.coilix.boot.StartupError.Configuration]], and a class without `@CoilixApplication` a
 * [[dev.coilix.boot.StartupError.NotABootApplication]]. Options of the JVM that make the application fragile when
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

  override def start[T: ClassTag](
    args:       List[String],
    singletons: List[RegisteredBean]
  ): ResourceT[StartupError, StartedApplication] =
    for {
      startedAt    <- lifted(IO.monotonic)
      arguments    <- lifted(IO(parser.parse(args)))
      environment  <- loaded(arguments)
      startup      <- lifted(IO.blocking(starting[T](environment)))
      basePackages <- EitherT.fromEither[[x] =>> Resource[IO, x]](startup.resolver.resolve[T])
      context      <- startup.bootstrap.start(basePackages, Nil, startupBeans(arguments, environment) ++ singletons)
      elapsed      <- lifted(IO.monotonic.map(_ - startedAt))
      _            <- lifted(IO(startup.startupLogger.started(applicationName[T], elapsed)))
    } yield StartedApplication(context, arguments)

  private def lifted[A](effect: IO[A]): ResourceT[StartupError, A] =
    EitherT.liftF(Resource.eval(effect))

  private def loaded(arguments: ApplicationArguments): ResourceT[StartupError, Environment] =
    EitherT(
      Resource.eval(IO.blocking(environmentLoader.load(commandLineProperties(arguments))).attemptNarrow[ConfigurationException])
    )
      .leftMap(StartupError.Configuration(_))

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
