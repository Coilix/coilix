package io.github.coilix.config

import io.github.coilix.common.ClassTags.runtimeTypeOf
import java.lang.reflect.Type
import scala.reflect.ClassTag

final class DefaultEnvironment(tree: ConfigTree, binder: ConfigBinder) extends Environment {

  private val literalKey = "literal"

  override def contains(path: String): Boolean =
    tree.contains(path)

  override def get[T: ClassTag](path: String): Option[T] =
    Option.when(contains(path))(typed[T](path, bind(path, runtimeTypeOf[T])))

  override def bind(path: String, targetType: Type): AnyRef =
    binder.bind(tree, path, targetType)

  override def resolve(expression: ValueExpression, targetType: Type): AnyRef =
    expression match {
      case ValueExpression.Literal(value)            =>
        binder.bind(ConfigTree.empty.withFallback(Map(literalKey -> value)), literalKey, targetType)
      case ValueExpression.Placeholder(key, default) => binder.bind(withDefault(key, default), key, targetType)
    }

  private def withDefault(key: String, default: Option[String]): ConfigTree =
    default.fold(tree)(value => tree.withFallback(Map(key -> value)))

  private def typed[T: ClassTag](path: String, value: AnyRef): T =
    summon[ClassTag[T]].unapply(value).getOrElse(throw mismatch(path, value))

  private def mismatch[T: ClassTag](path: String, value: AnyRef): ConfigurationException =
    ConfigurationException(s"Configuration '$path' is ${value.getClass.getName}, not ${runtimeTypeOf[T].getName}")
}
