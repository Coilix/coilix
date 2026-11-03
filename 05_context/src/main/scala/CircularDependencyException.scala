package dev.coilix.context

import dev.coilix.common.CoilixException

final class CircularDependencyException(beanNames: List[String])
    extends CoilixException(s"Circular dependency detected among: ${beanNames.mkString(", ")}")
