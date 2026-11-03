package dev.coilix.context

import dev.coilix.common.CoilixException

/**
 * Thrown when a component class has no public constructor, so Coilix cannot create it.
 *
 * @param beanClass
 *   the component class
 */
final class NoPublicConstructorException(beanClass: Class[?])
    extends CoilixException(s"Bean class ${beanClass.getName} has no public constructor")
