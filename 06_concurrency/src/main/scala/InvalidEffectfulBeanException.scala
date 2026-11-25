package dev.coilix.concurrency

import dev.coilix.common.CoilixException

/**
 * Thrown at startup when a bean made by an `IO` or a `Resource`, or one whose lifecycle methods return `IO[Unit]`, is marked
 * `@Lazy` or `@Prototype`: its effects run when the context starts and closes, so that no effect is run in the background
 * later.
 *
 * @param beanName
 *   the name of the bean
 */
final class InvalidEffectfulBeanException(beanName: String)
    extends CoilixException(
      s"Bean '$beanName' is made or initialized by an effect, so it cannot be @Lazy or @Prototype: its effects run when the " +
        "context starts and closes"
    )
