package dev.coilix.config

import cats.syntax.all.*
import dev.coilix.common.ClassTags.runtimeTypeOf
import dev.coilix.config.ConfigError.orThrow
import java.lang.reflect.Type
import scala.reflect.ClassTag

/**
 * An [[dev.coilix.config.Environment]] that reads values from a [[dev.coilix.config.ConfigTree]] and converts them with a
 * [[dev.coilix.config.ConfigBinder]].
 *
 * This is the implementation returned by [[dev.coilix.config.Environment.of]].
 */
case class DefaultEnvironment(tree: ConfigTree, binder: ConfigBinder) extends Environment {

  private val literalKey = "literal"

  override def contains(path: String): Boolean =
    tree.contains(path)

  override def activeProfiles: List[String] =
    Profiles.active(tree).orThrow

  override def lookup[T: ClassTag](path: String): Either[ConfigError, Option[T]] =
    Option.when(contains(path))(binding(path, runtimeTypeOf[T]).flatMap(typed[T](path, _))).sequence

  override def binding(path: String, targetType: Type): Either[ConfigError, AnyRef] =
    binder.bind(tree, path, targetType)

  override def evaluate(expression: ValueExpression, targetType: Type): Either[ConfigError, AnyRef] =
    expression match {
      case ValueExpression.Literal(value)            => binder.bind(literal(value), literalKey, targetType)
      case ValueExpression.Placeholder(key, default) => binder.bind(withDefault(key, default), key, targetType)
    }

  private def literal(value: String): ConfigTree =
    ConfigTree.empty.withFallback(Map(literalKey -> value))

  private def withDefault(key: String, default: Option[String]): ConfigTree =
    default.fold(tree)(value => tree.withFallback(Map(key -> value)))

  private def typed[T: ClassTag](path: String, value: AnyRef): Either[ConfigError, T] =
    summon[ClassTag[T]].unapply(value).toRight(ConfigError.TypeMismatch(path, value.getClass, runtimeTypeOf[T]))
}
