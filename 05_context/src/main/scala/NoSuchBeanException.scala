package io.github.coilix.context

import io.github.coilix.common.ClassTags.runtimeTypeOf
import io.github.coilix.common.CoilixException
import scala.reflect.ClassTag

final class NoSuchBeanException(message: String) extends CoilixException(message)

object NoSuchBeanException {

  /** Creates the exception reported when the context has no bean of type `T`. */
  def apply[T: ClassTag]: NoSuchBeanException = new NoSuchBeanException(s"No bean of type ${runtimeTypeOf[T].getName}")
}
