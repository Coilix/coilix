package org.scaling.boot

import org.scaling.common.ScalingException

final class NotABootApplicationException(applicationClass: Class[?])
    extends ScalingException(s"${applicationClass.getName} is not annotated with @ScalingBootApplication")
