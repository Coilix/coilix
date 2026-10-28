package org.scaling.config

import java.lang.reflect.Type
import scala.reflect.ClassTag

trait Environment {
  def contains(path: String): Boolean

  def get[T: ClassTag](path: String): Option[T]

  def bind(path: String, targetType: Type): AnyRef

  def resolve(expression: ValueExpression, targetType: Type): AnyRef
}

object Environment {
  def of(tree: ConfigTree): Environment = DefaultEnvironment(tree, ConfigBinder.default)

  def empty: Environment = of(ConfigTree.empty)
}
