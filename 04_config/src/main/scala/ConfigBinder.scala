package org.scaling.config

import java.lang.reflect.Type

trait ConfigBinder {
  def bind(tree: ConfigTree, path: String, targetType: Type): AnyRef
}

object ConfigBinder {
  def default: ConfigBinder =
    ReflectiveConfigBinder(DefaultScalarReader(), ScalaDefaultArguments(), Thread.currentThread().getContextClassLoader)
}
