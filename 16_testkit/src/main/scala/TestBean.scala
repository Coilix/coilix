package dev.coilix.testkit

import dev.coilix.common.GenericType

/**
 * A ready instance that a test puts into its context.
 *
 * @param beanName
 *   the name of the bean in the context
 * @param beanType
 *   the type the bean is registered and injected as
 * @param instance
 *   the instance
 * @param replaces
 *   whether the instance replaces the beans of `beanType` found by scanning, or is added next to them
 */
final case class TestBean(beanName: String, beanType: GenericType, instance: AnyRef, replaces: Boolean)
