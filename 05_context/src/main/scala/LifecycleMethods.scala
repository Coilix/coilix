package dev.coilix.context

import java.lang.reflect.Method

/**
 * The `@PostConstruct` and `@PreDestroy` methods of a bean, found by a [[dev.coilix.context.LifecycleMethodScanner]].
 *
 * Every method is already checked: it takes no parameters, can be invoked, also when it is private, and returns either `Unit` or
 * an effect of `Unit`, such as `IO[Unit]`. A method that returns an effect only describes its work, so it runs only when an
 * [[dev.coilix.context.EffectInterpreter]] creates the bean; a bean with such a method is effectful, as described in
 * [[dev.coilix.context.BeanDefinition.isEffectful]].
 *
 * @param init
 *   methods invoked after the bean is created, in this order
 * @param destroy
 *   methods invoked when the context closes, in this order
 */
final case class LifecycleMethods(init: List[Method], destroy: List[Method]) {

  /** The `@PostConstruct` methods that return `Unit`, which the [[dev.coilix.context.BeanPostProcessor]] invokes. */
  def plainInit: List[Method] = init.filterNot(LifecycleMethods.returnsEffect)

  /** The `@PostConstruct` methods that return an effect, which an [[dev.coilix.context.EffectInterpreter]] runs. */
  def effectfulInit: List[Method] = init.filter(LifecycleMethods.returnsEffect)

  /** The `@PreDestroy` methods that return `Unit`, which the [[dev.coilix.context.BeanDestroyer]] invokes. */
  def plainDestroy: List[Method] = destroy.filterNot(LifecycleMethods.returnsEffect)

  /** The `@PreDestroy` methods that return an effect, which an [[dev.coilix.context.EffectInterpreter]] runs. */
  def effectfulDestroy: List[Method] = destroy.filter(LifecycleMethods.returnsEffect)

  /** Whether any method returns an effect. */
  def hasEffects: Boolean = (init ++ destroy).exists(LifecycleMethods.returnsEffect)

  /**
   * These methods, when none of them returns an effect.
   *
   * @throws dev.coilix.context.BeanCreationException
   *   if a method returns an effect, because the bean `beanName` is created where no effect can run, such as on lookup of a lazy
   *   bean or by the synchronous context builder
   */
  def withoutEffects(beanName: String): LifecycleMethods =
    if (!hasEffects) this
    else {
      val names = (effectfulInit ++ effectfulDestroy).map(_.getName).distinct.mkString(", ")
      throw BeanCreationException(
        s"Bean '$beanName' has lifecycle methods that return effects ($names), which run only when an effect module creates the " +
          "bean at startup. Declare them on the bean type, not only on its implementation, and do not mark the bean @Lazy or " +
          "@Prototype",
        None
      )
    }
}

/** Factories for [[dev.coilix.context.LifecycleMethods]]. */
object LifecycleMethods {

  /** No lifecycle methods, for beans created outside component scanning such as the context itself. */
  val none: LifecycleMethods = LifecycleMethods(Nil, Nil)

  /** Whether `method`, a checked lifecycle method, returns an effect instead of `Unit`. */
  def returnsEffect(method: Method): Boolean = method.getReturnType != Void.TYPE
}
