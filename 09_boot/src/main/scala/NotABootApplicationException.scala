package io.github.coilix.boot

import io.github.coilix.common.CoilixException

final class NotABootApplicationException(applicationClass: Class[?])
    extends CoilixException(s"${applicationClass.getName} is not annotated with @CoilixApplication")
