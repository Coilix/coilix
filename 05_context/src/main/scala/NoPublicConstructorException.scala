package org.scaling.context

import org.scaling.common.ScalingException

final class NoPublicConstructorException(beanClass: Class[?])
    extends ScalingException(s"Bean class ${beanClass.getName} has no public constructor")
