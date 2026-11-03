package dev.coilix.context

import dev.coilix.common.CoilixException

final class NoPublicConstructorException(beanClass: Class[?])
    extends CoilixException(s"Bean class ${beanClass.getName} has no public constructor")
