package dev.coilix.context

import dev.coilix.common.CoilixException
import dev.coilix.common.TypeInfo

/**
 * Thrown when a requested bean, or a required dependency of a bean, does not exist.
 *
 * The message names the missing type and, for a dependency, the bean that needs it.
 */
final class NoSuchBeanException(message: String) extends CoilixException(message)

/** Factories for [[dev.coilix.context.NoSuchBeanException]]. */
object NoSuchBeanException {

  /** Creates the exception reported when the context has no bean of type `T`. */
  def apply[T: TypeInfo]: NoSuchBeanException = new NoSuchBeanException(s"No bean of type ${TypeInfo[T].genericType}")
}
