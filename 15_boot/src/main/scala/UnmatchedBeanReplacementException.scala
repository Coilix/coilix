package dev.coilix.boot

import dev.coilix.common.CoilixException
import dev.coilix.common.GenericType

/**
 * Thrown when a bean replacement matches no discovered bean, which usually means the wrong type was given.
 *
 * @param types
 *   the replaced types that match no discovered bean
 */
final class UnmatchedBeanReplacementException(types: List[GenericType])
    extends CoilixException(
      s"No discovered bean to replace for ${types.mkString(", ")}. " +
        "Replace a type that the application defines, or add the bean instead of replacing it"
    )
