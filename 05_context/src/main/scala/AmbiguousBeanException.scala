package dev.coilix.context

import dev.coilix.common.CoilixException

final class AmbiguousBeanException(requested: Class[?], candidates: List[String])
    extends CoilixException(
      s"Multiple beans of type ${requested.getName}: ${candidates.mkString(", ")}. Use @Primary or @Qualifier."
    )
