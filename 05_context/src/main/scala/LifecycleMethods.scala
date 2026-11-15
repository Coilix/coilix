package dev.coilix.context

import java.lang.reflect.Method

/**
 * The `@PostConstruct` and `@PreDestroy` methods of a bean, found by a [[dev.coilix.context.LifecycleMethodScanner]].
 *
 * Every method is already checked: it takes no parameters, returns `Unit` and can be invoked, also when it is private.
 *
 * @param init
 *   methods invoked after the bean is created, in this order
 * @param destroy
 *   methods invoked when the context closes, in this order
 */
final case class LifecycleMethods(init: List[Method], destroy: List[Method])

/** Factories for [[dev.coilix.context.LifecycleMethods]]. */
object LifecycleMethods {

  /** No lifecycle methods, for beans created outside component scanning such as the context itself. */
  val none: LifecycleMethods = LifecycleMethods(Nil, Nil)
}
