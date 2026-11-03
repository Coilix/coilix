package dev.coilix.boot

import dev.coilix.common.CoilixException

final class NotABootApplicationException(applicationClass: Class[?])
    extends CoilixException(s"${applicationClass.getName} is not annotated with @CoilixApplication")
