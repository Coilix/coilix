package dev.coilix.context

import cats.effect.IO
import cats.effect.Resource
import dev.coilix.common.CoilixException
import dev.coilix.common.GenericType

/**
 * A predictable reason why a context cannot be planned or created, returned as the left side of
 * [[dev.coilix.context.ContextPlanner.plan]] and [[dev.coilix.context.ApplicationContextBuilder.create]].
 *
 * The caller decides what to do with it: [[dev.coilix.context.ApplicationContextBuilder.build]] throws its
 * [[dev.coilix.context.ContextError.exception]], and the boot module wraps it in its own startup error. A defect, such as a bug in
 * a component, is not a `ContextError`: it fails the effect instead.
 */
enum ContextError {

  /**
   * A required dependency, a qualified dependency or a name in `dependsOn` matches no bean.
   *
   * @param description
   *   names the missing bean and the bean that needs it
   */
  case MissingBean(description: String)

  /**
   * Several beans match a requested type and none of them is the only primary one.
   *
   * @param requested
   *   the requested type
   * @param candidates
   *   the names of the matching beans
   */
  case AmbiguousBean(requested: GenericType, candidates: List[String])

  /**
   * Beans depend on each other in a cycle.
   *
   * @param beanNames
   *   the beans that could not be ordered, including the ones in the cycle
   */
  case CircularDependency(beanNames: List[String])

  /**
   * A bean depends on another bean in a way that cannot be satisfied, such as on a prototype through `dependsOn`.
   *
   * @param description
   *   names both beans and the reason
   */
  case InvalidDependency(description: String)

  /**
   * Beans are created or initialized by effects, but the context is built without an
   * [[dev.coilix.context.EffectInterpreter]].
   *
   * @param definitions
   *   the effectful beans, in creation order
   */
  case EffectfulBeans(definitions: List[BeanDefinition])

  /**
   * Coilix reported an error while it created a bean, for example a factory or `@PostConstruct` method threw, or a configuration
   * value could not be bound.
   *
   * @param failure
   *   the error reported while the bean was created
   */
  case CreationFailed(failure: CoilixException)

  /** The exception that reports this error to code that throws, such as [[dev.coilix.context.ApplicationContextBuilder.build]]. */
  def exception: CoilixException =
    this match {
      case MissingBean(description)        => new NoSuchBeanException(description)
      case AmbiguousBean(requested, names) => AmbiguousBeanException(requested, names)
      case CircularDependency(beanNames)   => CircularDependencyException(beanNames)
      case InvalidDependency(description)  => BeanCreationException(description)
      case EffectfulBeans(definitions)     => EffectfulBeanException(definitions)
      case CreationFailed(failure)         => failure
    }

  /** The description of this error, the message of its [[dev.coilix.context.ContextError.exception]]. */
  def message: String = exception.getMessage
}

/** Raising a [[dev.coilix.context.ContextError]] for callers that cannot handle it. */
object ContextError {

  /** The value of `created`, failing the resource with the [[dev.coilix.context.ContextError.exception]] of an error. */
  def raised[A](created: ResourceT[ContextError, A]): Resource[IO, A] =
    created.valueOrF(error => Resource.raiseError[IO, A, Throwable](error.exception))
}
