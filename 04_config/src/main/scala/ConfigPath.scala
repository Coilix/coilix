package org.scaling.config

import org.scaling.common.ClassTags.runtimeTypeOf
import scala.reflect.ClassTag

final case class ConfigPath[T](path: String, componentType: Class[T])

object ConfigPath {
  def apply[T: ClassTag](path: String): ConfigPath[T] = ConfigPath(path, runtimeTypeOf[T])
}
