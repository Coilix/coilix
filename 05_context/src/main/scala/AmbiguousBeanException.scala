package org.scaling.context

import org.scaling.common.ScalingException

final class AmbiguousBeanException(requested: Class[?], candidates: List[String])
    extends ScalingException(
      s"Multiple beans of type ${requested.getName}: ${candidates.mkString(", ")}. Use @Primary or @Qualifier."
    )
