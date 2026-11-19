package dev.coilix.boot

import dev.coilix.common.CoilixException

/**
 * Thrown when a class has a component annotation and also an annotation collected by a module, so it would be a bean and a
 * collected component at the same time.
 *
 * @param componentClass
 *   the class with both annotations
 * @param annotations
 *   the simple names of the collected annotations on the class
 */
final class CollectedComponentConflictException(componentClass: Class[?], annotations: String)
    extends CoilixException(
      s"Class ${componentClass.getName} is marked with @$annotations, which a module collects, so it cannot be a bean too. " +
        "Remove its component annotation"
    )
