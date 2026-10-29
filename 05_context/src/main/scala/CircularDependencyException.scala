package io.github.coilix.context

import io.github.coilix.common.CoilixException

final class CircularDependencyException(beanNames: List[String])
    extends CoilixException(s"Circular dependency detected among: ${beanNames.mkString(", ")}")
