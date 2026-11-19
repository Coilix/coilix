package dev.coilix.context

import dev.coilix.common.CoilixException
import dev.coilix.common.GenericType

/**
 * Thrown when a single bean of a type is requested and several beans match, none of them marked with `@Primary`.
 *
 * The message lists the names of the matching beans. Mark one of them with `@Primary`, or inject `List[T]` to get all of
 * them.
 *
 * @param requested
 *   the requested type
 * @param candidates
 *   names of the matching beans
 */
final class AmbiguousBeanException(requested: GenericType, candidates: List[String])
    extends CoilixException(
      s"Multiple beans of type $requested: ${candidates.mkString(", ")}. Use @Primary or @Qualifier."
    )
