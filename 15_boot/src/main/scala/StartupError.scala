package dev.coilix.boot

import cats.effect.IO
import cats.effect.Resource
import dev.coilix.common.CoilixException
import dev.coilix.common.GenericType
import dev.coilix.config.ConfigurationException
import dev.coilix.context.ContextError
import dev.coilix.context.NoSuchBeanException
import dev.coilix.context.ResourceT

/**
 * A predictable reason why an application cannot start, returned as the left side of
 * [[dev.coilix.boot.CoilixBootstrap.start]] and [[dev.coilix.boot.CoilixApplicationRunner.start]].
 *
 * It keeps the [[dev.coilix.context.ContextError]] of the container in [[dev.coilix.boot.StartupError.Context]], so the caller
 * sees what went wrong in every layer. [[dev.coilix.boot.CoilixIOApp]] logs it and ends the process with an error exit code,
 * and synchronous callers throw its [[dev.coilix.boot.StartupError.exception]]. A defect, such as a bug in a component, is not a
 * `StartupError`: it fails the effect instead.
 */
enum StartupError {

  /**
   * The class of the application is not annotated with `@CoilixApplication`.
   *
   * @param applicationClass
   *   the class of the application
   */
  case NotABootApplication(applicationClass: Class[?])

  /**
   * More than one enabled module gives a [[dev.coilix.context.EffectInterpreter]].
   *
   * @param modules
   *   the names of the modules that give an interpreter
   */
  case ConflictingEffectInterpreters(modules: List[String])

  /**
   * A bean replacement matches no discovered bean.
   *
   * @param types
   *   the replaced types that match no discovered bean
   */
  case UnmatchedBeanReplacement(types: List[GenericType])

  /**
   * A class has a component annotation and also an annotation collected by a module.
   *
   * @param componentClass
   *   the class with both annotations
   * @param annotations
   *   the simple names of the collected annotations on the class
   */
  case CollectedComponentConflict(componentClass: Class[?], annotations: String)

  /**
   * A bean requires a class that a module collects, which is therefore not a bean.
   *
   * @param beanName
   *   the bean with the dependency
   * @param componentClass
   *   the collected class it requires
   * @param annotations
   *   the simple names of the collected annotations on that class
   */
  case CollectedComponentRequired(beanName: String, componentClass: Class[?], annotations: String)

  /**
   * The configuration of the application cannot be loaded.
   *
   * @param failure
   *   the error reported while the configuration was loaded
   */
  case Configuration(failure: ConfigurationException)

  /**
   * The context cannot be planned or created.
   *
   * @param error
   *   the error of the container
   */
  case Context(error: ContextError)

  /** The exception that reports this error to code that throws. */
  def exception: CoilixException =
    this match {
      case NotABootApplication(applicationClass)                       => NotABootApplicationException(applicationClass)
      case ConflictingEffectInterpreters(modules)                      => ConflictingEffectInterpretersException(modules)
      case UnmatchedBeanReplacement(types)                             => UnmatchedBeanReplacementException(types)
      case CollectedComponentConflict(componentClass, names)           => CollectedComponentConflictException(componentClass, names)
      case CollectedComponentRequired(beanName, componentClass, names) => notABean(beanName, componentClass, names)
      case Configuration(failure)                                      => failure
      case Context(error)                                              => error.exception
    }

  /** The description of this error, the message of its [[dev.coilix.boot.StartupError.exception]]. */
  def message: String = exception.getMessage

  private def notABean(beanName: String, componentClass: Class[?], annotations: String): NoSuchBeanException =
    new NoSuchBeanException(
      s"Bean '$beanName' requires '${componentClass.getName}', which is marked with @$annotations and collected by a module, " +
        "so it is not a bean. Inject the module bean that manages it instead"
    )
}

/** Raising a [[dev.coilix.boot.StartupError]] for callers that cannot handle it. */
object StartupError {

  /** The value of `started`, failing the resource with the [[dev.coilix.boot.StartupError.exception]] of an error. */
  def raised[A](started: ResourceT[StartupError, A]): Resource[IO, A] =
    started.valueOrF(error => Resource.raiseError[IO, A, Throwable](error.exception))
}
