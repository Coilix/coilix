package dev.coilix.catseffect

import dev.coilix.common.CoilixException

/**
 * Thrown at startup when a bean made by an `IO` or a `Resource` is marked `@Lazy` or `@Prototype`: its effect runs once, when the
 * context starts, so that no effect is run in the background later.
 *
 * @param beanName
 *   the name of the bean
 */
final class InvalidEffectfulBeanException(beanName: String)
    extends CoilixException(
      s"Bean '$beanName' is made by an effect, so it cannot be @Lazy or @Prototype: its effect runs once, when the context starts"
    )
