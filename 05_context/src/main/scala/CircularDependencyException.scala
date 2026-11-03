package dev.coilix.context

import dev.coilix.common.CoilixException

/**
 * Thrown when beans depend on each other in a cycle, directly or through `Option[T]` dependencies.
 *
 * Coilix injects only through constructors, so a cycle cannot be created. Break it by moving the shared logic to a new bean,
 * or by looking up one side through [[dev.coilix.context.ApplicationContext]] when it is used.
 *
 * @param beanNames
 *   the beans that could not be created, including the ones in the cycle
 */
final class CircularDependencyException(beanNames: List[String])
    extends CoilixException(s"Circular dependency detected among: ${beanNames.mkString(", ")}")
