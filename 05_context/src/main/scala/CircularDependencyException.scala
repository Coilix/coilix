package org.scaling.context

import org.scaling.common.ScalingException

final class CircularDependencyException(beanNames: List[String])
    extends ScalingException(s"Circular dependency detected among: ${beanNames.mkString(", ")}")
